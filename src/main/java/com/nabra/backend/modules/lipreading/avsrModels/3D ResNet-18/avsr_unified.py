"""
Unified AVSR: Audio-Visual Speech Recognition with simultaneous audio and lip reading.
Uses separate venv for audio (audio model/.venv) to avoid dependency conflicts.
"""
import argparse
import json
import re
import subprocess
import sys
import time
from collections import deque
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Optional, Tuple

import cv2
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
from torchvision.models.video import r3d_18

# MediaPipe for precise lip ROI
try:
    import mediapipe as mp
    from mediapipe.tasks import python
    from mediapipe.tasks.python import vision
    HAS_MEDIAPIPE = True
except Exception:
    HAS_MEDIAPIPE = False

# ==================== LIP READING MODEL ====================

MOUTH_LANDMARKS = [
    61, 185, 40, 39, 37, 0, 267, 269, 270, 409,
    146, 91, 181, 84, 17, 314, 405, 321, 375, 291,
    78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
    95, 88, 178, 87, 14, 317, 402, 318, 324, 308
]


def get_lip_model(num_classes: int):
    """3D ResNet-18 for lip reading"""
    model = r3d_18(weights=None)
    in_features = model.fc.in_features
    model.fc = nn.Sequential(
        nn.Identity(),
        nn.Linear(in_features, num_classes)
    )
    return model


def load_word_map(word_to_idx_path: Path):
    """Load word vocabulary mapping"""
    with open(word_to_idx_path, 'r', encoding='utf-8') as f:
        word_to_idx = json.load(f)
    idx_to_word = {v: k for k, v in word_to_idx.items()}
    return word_to_idx, idx_to_word


def crop_mouth_from_frame(frame, landmarks):
    """Crop mouth ROI using MediaPipe face landmarks"""
    h, w, _ = frame.shape
    xs, ys = [], []

    for idx in MOUTH_LANDMARKS:
        if idx < len(landmarks):
            lm = landmarks[idx]
            xs.append(int(lm.x * w))
            ys.append(int(lm.y * h))

    if not xs or not ys:
        return None

    x1 = max(min(xs) - 4, 0)
    y1 = max(min(ys) - 4, 0)
    x2 = min(max(xs) + 4, w)
    y2 = min(max(ys) + 4, h)

    if x2 <= x1 or y2 <= y1:
        return None

    mouth = frame[y1:y2, x1:x2]
    if mouth.size == 0:
        return None
    
    mouth_h, mouth_w = mouth.shape[:2]
    if mouth_h < 20 or mouth_w < 20 or mouth_h > 300 or mouth_w > 300:
        return None

    return mouth


def preprocess_frame(frame, img_size):
    """Preprocess frame: resize to 160x100, then 112x112, normalize"""
    if frame is None or frame.size == 0:
        return None
    
    mouth_resized = cv2.resize(frame, (160, 100), interpolation=cv2.INTER_CUBIC)
    resized = cv2.resize(mouth_resized, (img_size, img_size), interpolation=cv2.INTER_CUBIC)
    rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
    arr = rgb.astype(np.float32) / 255.0
    arr = np.transpose(arr, (2, 0, 1))  # C, H, W
    return arr


def predict_lip_reading(model, frames, device, idx_to_word, top_k=5):
    """Run lip reading prediction and return top-k results"""
    frames_array = np.stack(frames, axis=1)  # C, T, H, W
    tensor = torch.from_numpy(frames_array).unsqueeze(0).to(device)  # 1, C, T, H, W
    
    with torch.inference_mode():
        logits = model(tensor)
        probs = F.softmax(logits, dim=1)[0]
        conf, idx = torch.max(probs, dim=0)
    
    probs_np = probs.cpu().numpy()
    top_indices = np.argsort(probs_np)[::-1][:top_k]
    top_predictions = [(idx_to_word[int(i)], float(probs_np[i])) for i in top_indices]
    
    return idx_to_word[idx.item()], conf.item(), top_predictions


# ==================== AUDIO ASR MODEL ====================

def start_audio_asr_subprocess(duration: float = 2.0, device_id=None):
    """
    Start audio ASR subprocess immediately (non-blocking).
    Returns the subprocess Popen object and result file path.
    
    Args:
        duration: Recording duration in seconds
        device_id: Audio device ID (None=default, 3=Realtek to bypass SteelSeries)
    """
    script_dir = Path(__file__).parent
    audio_model_dir = script_dir.parent / "audio model"
    audio_venv_python = audio_model_dir / ".venv" / "Scripts" / "python.exe"
    audio_script = audio_model_dir / "test_asr_ctc.py"
    result_file = audio_model_dir / f"realtime_result_{int(time.time() * 1000)}.txt"
    
    # Check paths exist
    if not audio_venv_python.exists():
        print(f"[ERROR] Audio model Python not found: {audio_venv_python}")
        return None, result_file
    if not audio_script.exists():
        print(f"[ERROR] Audio script not found: {audio_script}")
        return None, result_file
    
    # Clear previous result file
    if result_file.exists():
        try:
            result_file.unlink()
        except Exception:
            pass
    
    # Build service command with optional device parameter
    cmd = [
        str(audio_venv_python),
        str(audio_script),
        "--mic-service",
        "--duration",
        str(duration),
    ]
    if device_id is not None:
        cmd.extend(["--device", str(device_id)])
    
    # Start persistent subprocess service (non-blocking)
    try:
        print(f"[INFO] Starting audio ASR service (duration={duration}s, device={device_id})...")
        
        process = subprocess.Popen(
            cmd,
            cwd=str(audio_model_dir),
            stdin=subprocess.PIPE,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding='utf-8',
            errors='replace'
        )
        time.sleep(0.2)
        if process.poll() is not None:
            print(f"[ERROR] Audio service exited early with code {process.returncode}")
            return None, result_file
        print("[OK] Audio ASR service ready (model loaded once)")
        return process, result_file
    except Exception as e:
        print(f"[ERROR] Failed to start audio subprocess: {e}")
        import traceback
        traceback.print_exc()
        return None, result_file


def trigger_audio_asr_recording(process, duration: float = 2.0) -> Optional[Path]:
    if process is None or process.poll() is not None or process.stdin is None:
        return None

    script_dir = Path(__file__).parent
    audio_model_dir = script_dir.parent / "audio model"
    result_file = audio_model_dir / f"realtime_result_{int(time.time() * 1000)}.txt"

    try:
        process.stdin.write(f"REC\t{duration}\t{result_file}\n")
        process.stdin.flush()
        return result_file
    except Exception:
        return None


def wait_audio_asr_result(process, result_file: Path, timeout: float = 10.0) -> str:
    """
    Wait for audio ASR subprocess to complete and return the transcribed text.
    """
    if process is None:
        print("[WARNING] Audio process is None, skipping audio")
        return ""
    
    start_time = time.time()
    while (time.time() - start_time) < timeout:
        if process.poll() is not None:
            print(f"[ERROR] Audio service stopped unexpectedly (code={process.returncode})")
            return ""

        if result_file.exists():
            try:
                lines = [line.strip() for line in result_file.read_text(encoding='utf-8').splitlines()]
                non_empty_lines = [line for line in lines if line]
                audio_text = non_empty_lines[-1] if non_empty_lines else ""
                if audio_text:
                    print(f"[OK] Audio ASR: {audio_text}")
                    return audio_text
            except Exception:
                pass

        time.sleep(0.05)

    print(f"[WARNING] Audio ASR timeout after {timeout}s")
    return ""


def stop_audio_asr_service(process) -> None:
    if process is None:
        return
    try:
        if process.poll() is None and process.stdin is not None:
            process.stdin.write("QUIT\n")
            process.stdin.flush()
    except Exception:
        pass
    try:
        process.terminate()
        process.wait(timeout=1.5)
    except Exception:
        try:
            process.kill()
        except Exception:
            pass


# ==================== SIMILARITY FUSION ====================

def levenshtein_distance(s1: str, s2: str) -> int:
    """Calculate Levenshtein distance between two strings"""
    if len(s1) < len(s2):
        return levenshtein_distance(s2, s1)
    if len(s2) == 0:
        return len(s1)
    
    previous_row = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        current_row = [i + 1]
        for j, c2 in enumerate(s2):
            insertions = previous_row[j + 1] + 1
            deletions = current_row[j] + 1
            substitutions = previous_row[j] + (c1 != c2)
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row
    
    return previous_row[-1]


def similarity_score(s1: str, s2: str) -> float:
    """Calculate similarity score (0-1) between two strings"""
    if not s1 or not s2:
        return 0.0
    dist = levenshtein_distance(s1.lower(), s2.lower())
    max_len = max(len(s1), len(s2))
    return 1.0 - (dist / max_len) if max_len > 0 else 0.0


def fuse_predictions(audio_text: str, lip_predictions: list) -> Tuple[str, float, str]:
    """
    Fuse audio and lip predictions using similarity matching.
    Returns: (best_word, confidence, fusion_reason)
    """
    if not audio_text or not lip_predictions:
        # Fallback to lip reading top prediction
        if lip_predictions:
            return lip_predictions[0][0], lip_predictions[0][1], "lip_only"
        return "", 0.0, "no_data"
    
    # Find best matching lip prediction
    best_match = None
    best_similarity = 0.0
    all_similarities = []
    
    for word, lip_conf in lip_predictions:
        sim = similarity_score(audio_text, word)
        all_similarities.append((word, sim))
        if sim > best_similarity:
            best_similarity = sim
            best_match = (word, lip_conf, sim)
    
    # Show top similarities for debugging
    print(f"[FUSION] Audio '{audio_text}' vs Lip Top-5 similarities:")
    for word, sim in all_similarities[:5]:
        print(f"        '{word}': {sim:.2%}")
    
    if best_match and best_similarity > 0.3:
        # Good match found
        word, lip_conf, sim = best_match
        fused_conf = (lip_conf + sim) / 2.0
        return word, fused_conf, f"fused_sim={sim:.2%}"
    else:
        # No good match, use audio if confident, else lip top-1
        print(f"[FUSION] Best similarity {best_similarity:.2%} < 30%, using lip prediction")
        if lip_predictions:
            return lip_predictions[0][0], lip_predictions[0][1], "lip_fallback"
        return audio_text, 0.5, "audio_fallback"


# ==================== MAIN LOOP ====================

def main():
    parser = argparse.ArgumentParser(description="Unified AVSR: Audio + Lip Reading in one script")
    parser.add_argument('--checkpoint', type=str, default='checkpoints/best_model_acc_82.59.pth', help='Lip model checkpoint')
    parser.add_argument('--word_map', type=str, default='checkpoints/word_to_idx.json', help='Word vocabulary')
    parser.add_argument('--asr_model', type=str, default='elgeish/wav2vec2-large-xlsr-53-levantine-arabic', help='ASR model ID')
    parser.add_argument('--camera', type=int, default=0, help='Webcam device ID')
    parser.add_argument('--frames', type=int, default=25, help='Frames per lip prediction')
    parser.add_argument('--img_size', type=int, default=112, help='Resize mouth to this size')
    parser.add_argument('--duration', type=float, default=2.2, help='Audio recording duration in seconds')
    parser.add_argument('--top_k', type=int, default=5, help='Show top-K lip predictions')
    parser.add_argument('--use_mediapipe', action='store_true', help='Use MediaPipe for precise lip ROI')
    parser.add_argument('--display', action='store_true', default=True, help='Show camera preview')
    parser.add_argument('--audio_device', type=int, default=3, help='Audio device ID (3=Realtek, None=default). Use 3 to bypass SteelSeries Sonar.')
    args = parser.parse_args()

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"[INFO] Device: {device}")

    # Load lip reading model
    print("[INFO] Loading lip reading model...")
    word_to_idx, idx_to_word = load_word_map(Path(args.word_map))
    num_classes = len(idx_to_word)
    lip_model = get_lip_model(num_classes)
    ckpt = torch.load(args.checkpoint, map_location=device)
    if isinstance(ckpt, dict) and 'model_state_dict' in ckpt:
        lip_model.load_state_dict(ckpt['model_state_dict'])
    else:
        lip_model.load_state_dict(ckpt)
    lip_model = lip_model.to(device).eval()
    print(f"[OK] Loaded checkpoint: {args.checkpoint}")

    # Warm-up lip model
    warmup = torch.zeros((1, 3, args.frames, args.img_size, args.img_size), device=device)
    with torch.inference_mode():
        _ = lip_model(warmup)

    # Audio ASR uses persistent subprocess service (model loaded once)
    print("[INFO] Starting audio ASR persistent service...")
    asr_enabled = True
    audio_service, _ = start_audio_asr_subprocess(duration=2.0, device_id=args.audio_device)
    if audio_service is None:
        asr_enabled = False
        print("[WARNING] Audio service unavailable, running in LIP-only mode")

    # Initialize MediaPipe
    detector = None
    if args.use_mediapipe and HAS_MEDIAPIPE:
        try:
            base_options = python.BaseOptions(model_asset_path="models/face_landmarker.task")
            options = vision.FaceLandmarkerOptions(
                base_options=base_options,
                num_faces=1,
                output_face_blendshapes=False,
                output_facial_transformation_matrixes=False
            )
            detector = vision.FaceLandmarker.create_from_options(options)
            print("[OK] MediaPipe Face Landmarker loaded")
        except Exception as e:
            print(f"[ERROR] MediaPipe init failed: {e}")

    # Open camera
    cap = cv2.VideoCapture(args.camera)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    if not cap.isOpened():
        raise RuntimeError(f"Cannot open camera {args.camera}")
    
    print("\n" + "="*60)
    print(">>> UNIFIED AVSR READY <<<")
    print("="*60)
    print("Press 'p' to capture audio + video simultaneously")
    print("Press 'q' to quit")
    print("="*60 + "\n")

    # State
    buffer = deque(maxlen=args.frames * 2)  # Allow extra frames for sampling
    recording = False
    recording_start_time = None
    video_duration = 1.0  # Video captures for 1 second
    audio_duration = 2.0  # Audio records for 2 seconds (via subprocess)
    last_good_mouth = None
    audio_result_file = None
    executor = ThreadPoolExecutor(max_workers=1)
    pending_future = None

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("[ERROR] Frame grab failed")
                break

            # Extract mouth ROI
            mouth = None
            if detector is not None:
                try:
                    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
                    result = detector.detect(mp_image)
                    
                    if result.face_landmarks and len(result.face_landmarks) > 0:
                        cropped = crop_mouth_from_frame(frame, result.face_landmarks[0])
                        if cropped is not None:
                            mouth = cv2.resize(cropped, (160, 100), interpolation=cv2.INTER_CUBIC)
                            last_good_mouth = mouth
                except Exception:
                    pass

            # Fallback to last good mouth
            if mouth is None and last_good_mouth is not None:
                mouth = last_good_mouth

            # Add frames to buffer during recording (only for 1 second)
            if recording and mouth is not None:
                elapsed = time.time() - recording_start_time
                if elapsed < video_duration:
                    arr = preprocess_frame(mouth, args.img_size)
                    if arr is not None:
                        buffer.append(arr)

            # Display
            if args.display:
                disp = frame.copy()
                if recording:
                    elapsed = time.time() - recording_start_time
                    status = f"REC {elapsed:.1f}s (V:{video_duration}s/A:{audio_duration}s)"
                else:
                    status = "Idle"
                buffer_info = f"Frames: {len(buffer)}"
                cv2.putText(disp, f"{buffer_info} | {status}", (10, 25), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                
                if pending_future is not None:
                    cv2.putText(disp, "PROCESSING...", (10, 55), 
                               cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 165, 255), 2)
                
                cv2.imshow('Unified AVSR', disp)

            # Handle key presses
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break

            # Check if prediction is done
            if pending_future is not None and pending_future.done():
                try:
                    result = pending_future.result()
                    print("\n" + "="*60)
                    print(">>> AVSR RESULT <<<")
                    print("="*60)
                    print(f"[AUDIO] {result['audio_text']}")
                    print(f"[LIP] Top-{len(result['lip_top'][:args.top_k])}:")
                    for i, (word, conf) in enumerate(result['lip_top'][:args.top_k], 1):
                        print(f"   {i}. {word} ({conf*100:.1f}%)")
                    print(f"[FUSED] {result['fused_word']} (conf={result['fused_conf']*100:.1f}%, reason={result['fusion_reason']})")
                    print("="*60 + "\n")
                except Exception as e:
                    print(f"[ERROR] Prediction failed: {e}")
                finally:
                    pending_future = None

            # Start recording when 'p' is pressed
            if key == ord('p') and not recording and pending_future is None:
                buffer.clear()
                recording = True
                recording_start_time = time.time()
                last_good_mouth = None
                
                # Trigger audio recording IMMEDIATELY (runs in parallel with video capture)
                if asr_enabled:
                    audio_trigger_time = time.time()
                    audio_result_file = trigger_audio_asr_recording(audio_service, duration=audio_duration)
                    print(f"\n{'='*60}")
                    print(f">>> RECORDING NOW - SPEAK ARABIC INTO MICROPHONE! <<<")
                    print(f"{'='*60}")
                    print(f"Video: {video_duration}s | Audio: {audio_duration}s")
                    print(f"Sync delta (audio-video): {(audio_trigger_time - recording_start_time) * 1000:.1f} ms")
                    if args.audio_device:
                        print(f"Microphone: Device {args.audio_device} (Realtek - bypasses SteelSeries)")
                    print(f"{'='*60}\n")
                else:
                    print(f"[REC] Recording started (video: {video_duration}s)...")

            # Stop video recording after 1 second (audio continues for 2 seconds total)
            if recording:
                elapsed = time.time() - recording_start_time
                if elapsed >= video_duration and len(buffer) > 0:
                    # Video capture done, now process
                    # Sample buffer to exactly args.frames (25 frames)
                    if len(buffer) >= args.frames:
                        indices = np.linspace(0, len(buffer) - 1, args.frames, dtype=int)
                        sampled_buffer = [buffer[i] for i in indices]
                        print(f"[OK] Captured {len(buffer)} frames in {video_duration}s, sampled to {args.frames} frames")
                    else:
                        sampled_buffer = list(buffer)
                        print(f"[WARNING] Only captured {len(buffer)} frames in {video_duration}s (expected ~{args.frames})")
                    
                    # Submit processing job
                    # Capture audio_result_file at this moment using default args
                    def process_avsr(captured_result_file=audio_result_file):
                        result = {}
                        
                        # Wait for triggered audio recording result
                        if asr_enabled and captured_result_file is not None:
                            print("[INFO] Waiting for audio ASR to complete...")
                            audio_text = wait_audio_asr_result(audio_service, captured_result_file, timeout=audio_duration + 6.0)
                        else:
                            audio_text = ""
                        
                        # Process lip reading (use sampled frames)
                        lip_word, lip_conf, lip_top = predict_lip_reading(
                            lip_model, sampled_buffer, device, idx_to_word, args.top_k)
                        
                        # Fuse predictions
                        fused_word, fused_conf, fusion_reason = fuse_predictions(
                            audio_text, lip_top)
                        
                        result['audio_text'] = audio_text
                        result['lip_word'] = lip_word
                        result['lip_conf'] = lip_conf
                        result['lip_top'] = lip_top
                        result['fused_word'] = fused_word
                        result['fused_conf'] = fused_conf
                        result['fusion_reason'] = fusion_reason
                        return result
                    
                    pending_future = executor.submit(process_avsr)
                    
                    # Reset recording state
                    buffer.clear()
                    recording = False
                    recording_start_time = None
                    last_good_mouth = None
                    audio_result_file = None

    finally:
        cap.release()
        if args.display:
            cv2.destroyAllWindows()
        executor.shutdown(wait=False)
        stop_audio_asr_service(audio_service)
        if detector is not None:
            try:
                detector.close()
            except Exception:
                pass


if __name__ == '__main__':
    main()
