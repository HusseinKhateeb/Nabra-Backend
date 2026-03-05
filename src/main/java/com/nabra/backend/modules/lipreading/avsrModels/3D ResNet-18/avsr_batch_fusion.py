
"""
Batch AVSR Fusion: Processes given audio and video files for unified speech recognition.
Designed for backend integration (no webcam, no manual triggers).
"""

# Force UTF-8 encoding for stdout to avoid UnicodeEncodeError on Windows
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import logging
import sys
import json
import os
import atexit
import threading
import importlib.util
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F
import cv2
import subprocess

try:
    import mediapipe as mp
    from mediapipe.tasks import python as mp_python
    from mediapipe.tasks.python import vision as mp_vision
    HAS_MEDIAPIPE = True
except Exception:
    mp = None
    mp_python = None
    mp_vision = None
    HAS_MEDIAPIPE = False

# --- Configurable paths ---
CHECKPOINT_PATH = 'checkpoints/best_model_acc_82.59.pth'
WORD_MAP_PATH = 'checkpoints/word_to_idx.json'
ASR_SCRIPT_PATH = '../audio model/test_asr_ctc.py'
ASR_VENV_PYTHON = os.getenv("AVSR_ASR_PYTHON", "")
ASR_PYTHON_CANDIDATES = [
    ASR_VENV_PYTHON,
    str((Path(__file__).resolve().parent.parent / "audio model" / ".venv" / "Scripts" / "python.exe")),
    "D:/Graduation Extra/Nabra Workspace/.venv/Scripts/python.exe",
    sys.executable,
]

MOUTH_LANDMARKS = [
    61, 185, 40, 39, 37, 0, 267, 269, 270, 409,
    146, 91, 181, 84, 17, 314, 405, 321, 375, 291,
    78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
    95, 88, 178, 87, 14, 317, 402, 318, 324, 308
]

_CACHED_DEVICE = None
_CACHED_MODEL = None
_CACHED_IDX_TO_WORD = None
_CACHED_FACE_DETECTOR = None
_CACHED_ASR = None
_ASR_LOCK = threading.Lock()
_FUSION_EXECUTOR = ThreadPoolExecutor(max_workers=2)
DEFAULT_FRAME_COUNT = max(8, int(os.getenv("AVSR_FRAME_COUNT", "25")))
ASR_SUBPROCESS_TIMEOUT_SECONDS = max(20, int(os.getenv("AVSR_ASR_TIMEOUT_SECONDS", "45")))
IMAGENET_MEAN = np.array([0.485, 0.456, 0.406], dtype=np.float32).reshape(3, 1, 1)
IMAGENET_STD = np.array([0.229, 0.224, 0.225], dtype=np.float32).reshape(3, 1, 1)


def fallback_mouth_crop(frame):
    if frame is None or frame.size == 0:
        return None
    h, w = frame.shape[:2]
    if h < 40 or w < 40:
        return None
    x1 = max(int(w * 0.25), 0)
    x2 = min(int(w * 0.75), w)
    y1 = max(int(h * 0.55), 0)
    y2 = min(int(h * 0.95), h)
    if x2 <= x1 or y2 <= y1:
        return None
    return frame[y1:y2, x1:x2]


def get_face_detector():
    global _CACHED_FACE_DETECTOR

    if not HAS_MEDIAPIPE:
        return None

    if _CACHED_FACE_DETECTOR is None:
        try:
            _CACHED_FACE_DETECTOR = mp_vision.FaceLandmarker.create_from_options(
                mp_vision.FaceLandmarkerOptions(
                    base_options=mp_python.BaseOptions(model_asset_path="models/face_landmarker.task"),
                    num_faces=1
                )
            )
        except Exception:
            _CACHED_FACE_DETECTOR = None
    return _CACHED_FACE_DETECTOR


def cleanup_cached_resources():
    global _CACHED_FACE_DETECTOR
    if _CACHED_FACE_DETECTOR is not None:
        try:
            _CACHED_FACE_DETECTOR.close()
        except Exception:
            pass
        _CACHED_FACE_DETECTOR = None
    _FUSION_EXECUTOR.shutdown(wait=False)


atexit.register(cleanup_cached_resources)

def load_word_map(word_to_idx_path):
    with open(word_to_idx_path, 'r', encoding='utf-8') as f:
        word_to_idx = json.load(f)
    idx_to_word = {v: k for k, v in word_to_idx.items()}
    return word_to_idx, idx_to_word

def get_lip_model(num_classes):
    from torchvision.models.video import r3d_18
    model = r3d_18(weights=None)
    in_features = model.fc.in_features
    model.fc = nn.Sequential(
        nn.Identity(),
        nn.Linear(in_features, num_classes)
    )
    return model

def extract_mouth_frames(video_path, img_size=112, frame_count=25):
    cap = cv2.VideoCapture(str(video_path))
    candidates = []
    detector = get_face_detector()
    detected_face_frames = 0
    last_good_mouth = None

    while True:
        ret, frame = cap.read()
        if not ret:
            break
        mouth = None
        if detector:
            try:
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
                result = detector.detect(mp_image)
                if result.face_landmarks and len(result.face_landmarks) > 0:
                    detected_face_frames += 1
                    xs, ys = [], []
                    for idx in MOUTH_LANDMARKS:
                        if idx < len(result.face_landmarks[0]):
                            lm = result.face_landmarks[0][idx]
                            xs.append(int(lm.x * frame.shape[1]))
                            ys.append(int(lm.y * frame.shape[0]))
                    if xs and ys:
                        x1 = max(min(xs) - 4, 0)
                        y1 = max(min(ys) - 4, 0)
                        x2 = min(max(xs) + 4, frame.shape[1])
                        y2 = min(max(ys) + 4, frame.shape[0])
                        mouth = frame[y1:y2, x1:x2]
            except Exception:
                pass
        if mouth is not None and mouth.size > 0:
            last_good_mouth = mouth
        elif last_good_mouth is not None:
            mouth = last_good_mouth
        else:
            mouth = fallback_mouth_crop(frame)

        if mouth is None or mouth.size == 0:
            continue

        mouth_resized = cv2.resize(mouth, (160, 100), interpolation=cv2.INTER_CUBIC)
        resized = cv2.resize(mouth_resized, (img_size, img_size), interpolation=cv2.INTER_CUBIC)
        rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
        arr = rgb.astype(np.float32) / 255.0
        arr = (arr - IMAGENET_MEAN.transpose(1, 2, 0)) / IMAGENET_STD.transpose(1, 2, 0)
        arr = np.transpose(arr, (2, 0, 1))
        candidates.append(arr)
    cap.release()

    if not candidates:
        return [], detected_face_frames, detector is not None

    if len(candidates) >= frame_count:
        start = (len(candidates) - frame_count) // 2
        frames = candidates[start:start + frame_count]
    else:
        frames = list(candidates)
        while len(frames) < frame_count:
            frames.append(frames[-1])

    return frames, detected_face_frames, detector is not None

def predict_lip(model, frames, device, idx_to_word, top_k=5):
    frames_array = np.stack(frames, axis=1)
    tensor = torch.from_numpy(frames_array).unsqueeze(0).to(device)
    with torch.inference_mode():
        logits = model(tensor)
        probs = F.softmax(logits, dim=1)[0]
        conf, idx = torch.max(probs, dim=0)
    probs_np = probs.cpu().numpy()
    top_indices = np.argsort(probs_np)[::-1][:top_k]
    top_predictions = [(idx_to_word[int(i)], float(probs_np[i])) for i in top_indices]
    return idx_to_word[idx.item()], conf.item(), top_predictions

def run_asr(audio_path):
    logging.basicConfig(filename='avsr_batch_fusion.log', level=logging.DEBUG, format='%(asctime)s %(levelname)s %(message)s')
    logging.debug(f"ASR audio_path: {audio_path}")
    logging.debug(f"ASR script path: {ASR_SCRIPT_PATH}")

    asr_script_file = (Path(__file__).resolve().parent / ASR_SCRIPT_PATH).resolve()
    try:
        global _CACHED_ASR
        with _ASR_LOCK:
            if _CACHED_ASR is None:
                spec = importlib.util.spec_from_file_location("nabra_test_asr_ctc", str(asr_script_file))
                if spec is None or spec.loader is None:
                    raise RuntimeError(f"Could not load ASR module spec: {asr_script_file}")
                asr_module = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(asr_module)
                model_id = os.getenv("AVSR_ASR_MODEL", "elgeish/wav2vec2-large-xlsr-53-levantine-arabic")
                processor, model, device = asr_module.load_asr(model_id)
                _CACHED_ASR = (asr_module, processor, model, device)

        asr_module, processor, model, device = _CACHED_ASR
        waveform, sr = asr_module.load_audio(str(audio_path))
        raw_text = asr_module.transcribe_waveform(waveform, sr, processor, model, device)
        if hasattr(asr_module, "normalize_asr_text"):
            normalized = asr_module.normalize_asr_text(raw_text)
        else:
            normalized = raw_text.strip()
        if normalized:
            return f"ASR (Arabic): {normalized}"
        return raw_text.strip()
    except Exception as e:
        logging.error(f"ASR in-process error: {e}")

    python_exec = next((candidate for candidate in ASR_PYTHON_CANDIDATES if candidate and Path(candidate).exists()), sys.executable)
    logging.debug(f"ASR fallback python exec: {python_exec}")
    try:
        result = subprocess.run([
            python_exec,
            str(asr_script_file),
            str(audio_path)
        ], capture_output=True, text=True, encoding='utf-8', timeout=ASR_SUBPROCESS_TIMEOUT_SECONDS)
        logging.debug(f"ASR fallback stdout: {result.stdout}")
        logging.debug(f"ASR fallback stderr: {result.stderr}")
        logging.debug(f"ASR fallback returncode: {result.returncode}")
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or result.stdout.strip() or f"ASR fallback exited with code {result.returncode}")
        return result.stdout.strip()
    except Exception as e:
        logging.error(f"ASR fallback error: {e}")
        return ""

def levenshtein(s1, s2):
    if len(s1) < len(s2):
        return levenshtein(s2, s1)
    if len(s2) == 0:
        return len(s1)
    prev_row = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        curr_row = [i + 1]
        for j, c2 in enumerate(s2):
            ins = prev_row[j + 1] + 1
            dels = curr_row[j] + 1
            subs = prev_row[j] + (c1 != c2)
            curr_row.append(min(ins, dels, subs))
        prev_row = curr_row
    return prev_row[-1]

def similarity(s1, s2):
    if not s1 or not s2:
        return 0.0
    dist = levenshtein(s1.lower(), s2.lower())
    max_len = max(len(s1), len(s2))
    return 1.0 - (dist / max_len) if max_len > 0 else 0.0

def fuse(audio_text, lip_predictions):
    if not audio_text or not lip_predictions:
        if lip_predictions:
            return lip_predictions[0][0], lip_predictions[0][1], "lip_only"
        return "", 0.0, "no_data"
    best_match = None
    best_sim = 0.0
    for word, lip_conf in lip_predictions:
        sim = similarity(audio_text, word)
        if sim > best_sim:
            best_sim = sim
            best_match = (word, lip_conf, sim)
    if best_match and best_sim > 0.3:
        word, lip_conf, sim = best_match
        fused_conf = (lip_conf + sim) / 2.0
        return word, fused_conf, f"fused_sim={sim:.2%}"
    else:
        if lip_predictions:
            return lip_predictions[0][0], lip_predictions[0][1], "lip_fallback"
        return audio_text, 0.5, "audio_fallback"


def extract_audio_text(asr_output):
    audio_text = ""
    for line in asr_output.splitlines():
        if line.strip().lower().startswith("asr (arabic):"):
            audio_text = line.split(":", 1)[-1].strip()
            if audio_text and any(ord(c) < 32 or ord(c) > 126 for c in audio_text):
                try:
                    audio_text = audio_text.encode('latin1').decode('utf-8')
                except Exception:
                    pass
            break
    if not audio_text:
        audio_text = asr_output.strip()
        if audio_text and any(ord(c) < 32 or ord(c) > 126 for c in audio_text):
            try:
                audio_text = audio_text.encode('latin1').decode('utf-8')
            except Exception:
                pass
    return audio_text


def run_fusion(audio_path, video_path, frame_count=None):
    global _CACHED_DEVICE, _CACHED_MODEL, _CACHED_IDX_TO_WORD

    if _CACHED_MODEL is None or _CACHED_IDX_TO_WORD is None or _CACHED_DEVICE is None:
        _CACHED_DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        _, _CACHED_IDX_TO_WORD = load_word_map(WORD_MAP_PATH)
        num_classes = len(_CACHED_IDX_TO_WORD)
        model = get_lip_model(num_classes)
        ckpt = torch.load(CHECKPOINT_PATH, map_location=_CACHED_DEVICE)
        if isinstance(ckpt, dict) and 'model_state_dict' in ckpt:
            model.load_state_dict(ckpt['model_state_dict'])
        else:
            model.load_state_dict(ckpt)
        _CACHED_MODEL = model.to(_CACHED_DEVICE).eval()
        with torch.inference_mode():
            warmup = torch.zeros((1, 3, 25, 112, 112), device=_CACHED_DEVICE)
            _CACHED_MODEL(warmup)

    asr_future = _FUSION_EXECUTOR.submit(run_asr, audio_path)
    effective_frame_count = DEFAULT_FRAME_COUNT if frame_count is None else max(8, int(frame_count))
    frames, detected_face_frames, face_detection_enabled = extract_mouth_frames(video_path, frame_count=effective_frame_count)
    if not frames:
        raise RuntimeError("No frames could be extracted from the video")
    if face_detection_enabled and detected_face_frames == 0:
        raise RuntimeError("No face detected in video frames")
    lip_word, lip_conf, lip_top = predict_lip(_CACHED_MODEL, frames, _CACHED_DEVICE, _CACHED_IDX_TO_WORD)
    asr_output = asr_future.result(timeout=ASR_SUBPROCESS_TIMEOUT_SECONDS + 20)
    audio_text = extract_audio_text(asr_output)
    fused_word, fused_conf, fusion_reason = fuse(audio_text, lip_top)
    return {
        "audio_text": audio_text,
        "lip_word": lip_word,
        "lip_conf": lip_conf,
        "lip_top": lip_top,
        "fused_word": fused_word,
        "fused_conf": fused_conf,
        "fusion_reason": fusion_reason
    }

def main(audio_path, video_path, frame_count=None):
    result = run_fusion(audio_path, video_path, frame_count=frame_count)
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    if len(sys.argv) < 3 or len(sys.argv) > 4:
        print("Usage: python avsr_batch_fusion.py <audio_file> <video_file> [frame_count]")
        sys.exit(1)
    fc = int(sys.argv[3]) if len(sys.argv) == 4 else None
    main(sys.argv[1], sys.argv[2], frame_count=fc)