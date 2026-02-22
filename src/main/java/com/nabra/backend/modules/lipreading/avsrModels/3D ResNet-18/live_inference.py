import argparse
from concurrent.futures import ThreadPoolExecutor
from collections import deque
from pathlib import Path
import time

import cv2
import numpy as np
import torch
import torch.nn.functional as F
import torch.nn as nn
from torchvision.models.video import r3d_18


def get_model(num_classes: int):
    model = r3d_18(weights=None)
    in_features = model.fc.in_features
    model.fc = nn.Sequential(
        nn.Identity(),
        nn.Linear(in_features, num_classes)
    )
    return model

# MediaPipe Face Landmarker for precise lip landmarks (same as preprocessing)
try:
    import mediapipe as mp
    from mediapipe.tasks import python
    from mediapipe.tasks.python import vision
    HAS_MEDIAPIPE = True
except Exception:
    mp = None
    HAS_MEDIAPIPE = False

# MediaPipe face mesh lip region landmarks (EXACT same as preprocessing script)
MOUTH_LANDMARKS = [
    61, 185, 40, 39, 37, 0, 267, 269, 270, 409,
    146, 91, 181, 84, 17, 314, 405, 321, 375, 291,
    78, 191, 80, 81, 82, 13, 312, 311, 310, 415,
    95, 88, 178, 87, 14, 317, 402, 318, 324, 308
]


def load_word_map(word_to_idx_path: Path):
    import json
    with open(word_to_idx_path, 'r', encoding='utf-8') as f:
        word_to_idx = json.load(f)
    idx_to_word = {v: k for k, v in word_to_idx.items()}
    return word_to_idx, idx_to_word


def crop_mouth_from_frame(frame, landmarks):
    """Crop mouth ROI using face landmarks (EXACT same as preprocessing script)."""
    h, w, _ = frame.shape
    xs, ys = [], []

    for idx in MOUTH_LANDMARKS:
        if idx < len(landmarks):
            lm = landmarks[idx]
            xs.append(int(lm.x * w))
            ys.append(int(lm.y * h))

    if not xs or not ys:
        return None

    # Extra tight crop with minimal padding (zoom to mouth only, EXACT same as preprocessing)
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
    if mouth_h < 20 or mouth_w < 20:
        return None
    
    if mouth_h > 300 or mouth_w > 300:
        return None

    return mouth


def preprocess_frame(frame, img_size):
    """Preprocess frame EXACTLY as training data: resize to 160x100, then to 112x112, BGR->RGB, normalize, transpose"""
    # Ensure frame is valid
    if frame is None or frame.size == 0:
        return None
    
    # Step 1: Resize to 160x100 (EXACT same as preprocessing script)
    mouth_resized = cv2.resize(frame, (160, 100), interpolation=cv2.INTER_CUBIC)
    
    # Step 2: Resize to model input size 112x112 (for model input)
    resized = cv2.resize(mouth_resized, (img_size, img_size), interpolation=cv2.INTER_CUBIC)
    
    # Convert BGR to RGB (exactly as training)
    rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
    # Normalize to [0, 1] (exactly as training)
    arr = rgb.astype(np.float32) / 255.0
    # Convert to (C, H, W) (exactly as training)
    arr = np.transpose(arr, (2, 0, 1))
    return arr


def predict_buffer(model, buffer, device):
    # buffer: deque of length T with arrays (C,H,W)
    frames = np.stack(buffer, axis=1)  # C, T, H, W
    tensor = torch.from_numpy(frames).unsqueeze(0).to(device)  # 1,C,T,H,W
    with torch.inference_mode():
        logits = model(tensor)
        probs = F.softmax(logits, dim=1)[0]
        conf, idx = torch.max(probs, dim=0)
    return idx.item(), conf.item(), probs.cpu().numpy()


def augment_frames(frames, aug_type='none'):
    """Apply test-time augmentation to frames for voting variation"""
    if aug_type == 'none':
        return frames
    
    # frames shape: (C, T, H, W)
    augmented = frames.copy()
    
    if aug_type == 'brightness':
        # Random brightness adjustment
        factor = np.random.uniform(0.85, 1.15)
        augmented = np.clip(augmented * factor, 0, 1)
    elif aug_type == 'contrast':
        # Random contrast adjustment
        factor = np.random.uniform(0.85, 1.15)
        mean = augmented.mean()
        augmented = np.clip((augmented - mean) * factor + mean, 0, 1)
    elif aug_type == 'temporal':
        # Random temporal sampling (slight frame shifts)
        T = frames.shape[1]
        shift = np.random.randint(-2, 3)  # shift -2 to +2 frames
        if shift > 0:
            augmented = np.concatenate([augmented[:, shift:], augmented[:, -shift:]], axis=1)
        elif shift < 0:
            augmented = np.concatenate([augmented[:, :shift], augmented[:, :abs(shift)]], axis=1)
    
    return augmented


def predict_with_voting(model, buffer, device, predict_count=3, use_tta=True):
    """Run multiple predictions with test-time augmentation and return most frequent result"""
    from collections import Counter
    
    # Original frames
    frames = np.stack(buffer, axis=1)  # C, T, H, W

    aug_batch = []
    for i in range(predict_count):
        if use_tta and i > 0:  # First prediction is always original
            # Apply different augmentations
            aug_types = ['none', 'brightness', 'contrast', 'temporal', 'brightness']
            aug_type = aug_types[i % len(aug_types)]
            aug_frames = augment_frames(frames, aug_type)
        else:
            aug_frames = frames

        aug_batch.append(aug_frames)

    # Predict all augmentations in one batch for lower latency
    batch = np.stack(aug_batch, axis=0)  # N, C, T, H, W
    tensor = torch.from_numpy(batch).to(device)
    with torch.inference_mode():
        logits = model(tensor)
        probs = F.softmax(logits, dim=1)
        confs, idxs = torch.max(probs, dim=1)

    predictions = idxs.detach().cpu().tolist()
    confidences = confs.detach().cpu().tolist()
    
    # Count votes
    vote_counts = Counter(predictions)
    # Get winner (most frequent)
    winner_idx = vote_counts.most_common(1)[0][0]
    winner_votes = vote_counts[winner_idx]
    # Average confidence of winner predictions
    winner_confs = [confidences[i] for i, pred in enumerate(predictions) if pred == winner_idx]
    avg_conf = sum(winner_confs) / len(winner_confs)
    
    return winner_idx, avg_conf, vote_counts, predictions


def run_prediction_job(model, frames, device, predict_count):
    idx, conf, probs = predict_buffer(model, frames, device)
    top_k = min(predict_count, len(probs))
    top_indices = np.argsort(probs)[::-1][:top_k]
    top_preds = [(int(i), float(probs[i])) for i in top_indices]
    return {
        'idx': idx,
        'conf': conf,
        'probs': probs,
        'top_preds': top_preds,
    }


def main():
    parser = argparse.ArgumentParser(description="Live Arabic lip reading with 3D ResNet-18")
    parser.add_argument('--checkpoint', type=str, required=True, help='Path to best_model_acc_XX.pth')
    parser.add_argument('--word_map', type=str, default='checkpoints/word_to_idx.json', help='Path to word_to_idx.json')
    parser.add_argument('--camera', type=int, default=0, help='Webcam device id')
    parser.add_argument('--frames', type=int, default=25, help='Frames per prediction (match training)')
    parser.add_argument('--img_size', type=int, default=112, help='Resize mouth to this size')
    parser.add_argument('--display', action='store_true', help='Show camera preview')
    parser.add_argument('--auto', action='store_true', help='Auto-predict as soon as buffer is full (continuous mode)')
    parser.add_argument('--show_roi', action='store_true', help='Show cropped mouth ROI in separate window')
    parser.add_argument('--use_mediapipe', action='store_true', help='Use MediaPipe FaceMesh for precise lip ROI')
    parser.add_argument('--pad', type=int, default=4, help='Padding around lip ROI in pixels')
    parser.add_argument('--roi_scale', type=float, default=1.25, help='Scale factor to expand lip bbox before cropping')
    parser.add_argument('--predict_count', type=int, default=3, help='Number of predictions for majority voting (1=single prediction)')
    parser.add_argument('--top_k', type=int, default=5, help='Show top-K predictions with confidence')
    parser.add_argument('--show_voting', action='store_true', help='Show all predictions and vote counts')
    parser.add_argument('--use_sliding_window', action='store_true', help='Use sliding window over continuous frames')
    parser.add_argument('--sliding_window_size', type=int, default=50, help='Total frames to keep in sliding window')
    parser.add_argument('--sliding_window_stride', type=int, default=5, help='Frames to advance between predictions')
    args = parser.parse_args()
    if args.top_k < 1:
        raise ValueError('top_k must be >= 1')

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Device: {device}")

    # Load mapping and model
    _, idx_to_word = load_word_map(Path(args.word_map))
    num_classes = len(idx_to_word)
    model = get_model(num_classes)
    ckpt = torch.load(args.checkpoint, map_location=device)
    if isinstance(ckpt, dict) and 'model_state_dict' in ckpt:
        model.load_state_dict(ckpt['model_state_dict'])
    else:
        model.load_state_dict(ckpt)
    model = model.to(device).eval()
    print(f"Loaded checkpoint: {args.checkpoint}")

    # Warm-up forward pass to reduce first prediction latency
    warmup = torch.zeros((1, 3, args.frames, args.img_size, args.img_size), device=device)
    with torch.inference_mode():
        _ = model(warmup)

    # Initialize MediaPipe Face Landmarker (EXACT same as preprocessing script)
    detector = None
    if args.use_mediapipe:
        if HAS_MEDIAPIPE:
            try:
                # Load MediaPipe Face Landmarker
                base_options = python.BaseOptions(
                    model_asset_path="models/face_landmarker.task"
                )
                options = vision.FaceLandmarkerOptions(
                    base_options=base_options,
                    num_faces=1,
                    output_face_blendshapes=False,
                    output_facial_transformation_matrixes=False
                )
                detector = vision.FaceLandmarker.create_from_options(options)
                print("✅ MediaPipe Face Landmarker loaded")
            except Exception as e:
                print(f"❌ MediaPipe Face Landmarker init failed: {e}")
                print("Make sure models/face_landmarker.task exists")
                detector = None
        else:
            print("MediaPipe not installed. Run: pip install mediapipe")

    if args.use_sliding_window and args.sliding_window_size < args.frames:
        raise ValueError('sliding_window_size must be >= frames')

    buffer_maxlen = args.sliding_window_size if args.use_sliding_window else args.frames
    buffer = deque(maxlen=buffer_maxlen)
    cap = cv2.VideoCapture(args.camera)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    if not cap.isOpened():
        raise RuntimeError(f"Cannot open camera {args.camera}")
    print("Camera opened. Press 'p' to capture 25-frame buffer, 'q' to quit.")

    last_pred = None
    last_conf = 0.0
    last_time = 0.0
    ready_for_next = True  # used for auto mode to avoid double predictions
    recording = False      # press 'p' to start capturing next 25 frames
    recording_raw_count = 0  # count raw captured frames in manual clip
    recording_failed_count = 0
    frames_since_pred = 0  # used for sliding window stride
    last_good_mouth = None  # EXACT same fallback behavior as dataset preprocessing
    pending_future = None
    pending_source = None
    executor = ThreadPoolExecutor(max_workers=1)

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("Frame grab failed")
                break

            # Extract mouth ROI using MediaPipe (EXACT same as preprocessing)
            mouth = None
            if detector is not None:
                try:
                    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    mp_image = mp.Image(
                        image_format=mp.ImageFormat.SRGB,
                        data=rgb
                    )
                    result = detector.detect(mp_image)
                    
                    if result.face_landmarks and len(result.face_landmarks) > 0:
                        cropped = crop_mouth_from_frame(frame, result.face_landmarks[0])
                        if cropped is not None:
                            mouth = cv2.resize(cropped, (160, 100), interpolation=cv2.INTER_CUBIC)
                            last_good_mouth = mouth
                except Exception as e:
                    pass  # Silently skip detection failures

            # EXACT same as dataset: fallback to last good mouth frame on failures
            if mouth is None and last_good_mouth is not None:
                mouth = last_good_mouth

            if recording:
                recording_raw_count += 1
                if mouth is None:
                    recording_failed_count += 1
            
            if mouth is not None and (recording or args.auto or args.use_sliding_window):
                arr = preprocess_frame(mouth, args.img_size)
                if arr is not None:
                    buffer.append(arr)
                    if args.use_sliding_window:
                        frames_since_pred += 1

            if args.display:
                disp = frame.copy()
                if args.use_sliding_window:
                    status = f"SLIDING({args.sliding_window_size})"
                    buffer_info = f"Buffer: {len(buffer)}/{args.sliding_window_size}"
                else:
                    status = "AUTO" if args.auto else ("RECORDING" if recording else "Idle (press 'p')")
                    buffer_info = f"Buffer: {len(buffer)}/{args.frames}"
                if pending_future is not None:
                    status = f"{status} | PREDICTING..."
                cv2.putText(disp, f"{buffer_info} | {status}", (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,255,0), 2)
                if last_pred is not None and (time.time() - last_time) < 3:
                    cv2.putText(disp, f"Pred: {last_pred} ({last_conf*100:.1f}%)", (10, 55), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,255,0), 2)
                cv2.imshow('Live Lip Reading', disp)

                # Optional ROI preview
                if args.show_roi and mouth is not None:
                    roi_disp = cv2.resize(mouth, (224, 224))
                    cv2.imshow('Mouth ROI', roi_disp)

            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break

            # Collect async prediction result without blocking camera loop
            if pending_future is not None and pending_future.done():
                try:
                    result = pending_future.result()
                    idx = result['idx']
                    conf = result['conf']
                    word = idx_to_word[idx]
                    top_preds = result['top_preds']
                    last_pred, last_conf, last_time = word, conf, time.time()
                    if pending_source == 'manual':
                        print(f"\n=== PREDICTION RESULT ===")
                        print(f"Prediction: {word} | Confidence: {conf*100:.2f}%")
                        print(f"Top {len(top_preds)} predictions:")
                        for rank, (pred_idx, pred_conf) in enumerate(top_preds, start=1):
                            print(f"  {rank}. {idx_to_word[pred_idx]} ({pred_conf*100:.2f}%)")
                        print("========================\n")
                    else:
                        print(f"Prediction: {word} | Confidence: {conf*100:.2f}%")
                        print("Top predictions: " + " | ".join(
                            [f"{idx_to_word[pred_idx]} {pred_conf*100:.1f}%" for pred_idx, pred_conf in top_preds]
                        ))
                except Exception as e:
                    print(f"Prediction failed: {e}")
                finally:
                    pending_future = None
                    pending_source = None

            # Manual mode: press 'p' to capture next full buffer and predict
            if not args.auto and not args.use_sliding_window and key == ord('p'):
                if not recording:
                    buffer.clear()
                    recording = True
                    recording_raw_count = 0
                    recording_failed_count = 0
                    last_good_mouth = None
                    ready_for_next = True
                    print("Recording started: capturing next frames...")
                else:
                    # cancel recording
                    buffer.clear()
                    recording = False
                    recording_raw_count = 0
                    recording_failed_count = 0
                    last_good_mouth = None
                    ready_for_next = True
                    print("Recording cancelled.")

            # Sliding window mode: predict on last args.frames every stride
            if (
                args.use_sliding_window and
                pending_future is None and
                len(buffer) >= args.frames and
                frames_since_pred >= args.sliding_window_stride
            ):
                window = list(buffer)[-args.frames:]
                pending_future = executor.submit(run_prediction_job, model, window, device, args.top_k)
                pending_source = 'sliding'
                frames_since_pred = 0

            # Manual mode: clip is exactly args.frames raw frames (EXACT same clip handling as dataset)
            if recording and recording_raw_count >= args.frames:
                if len(buffer) == args.frames and pending_future is None:
                    pending_future = executor.submit(run_prediction_job, model, list(buffer), device, args.top_k)
                    pending_source = 'manual'
                else:
                    print(f"Capture skipped: expected {args.frames} processed frames, got {len(buffer)} (failed detections: {recording_failed_count}). Try again.")
                buffer.clear()
                recording = False
                recording_raw_count = 0
                recording_failed_count = 0
                last_good_mouth = None
                ready_for_next = True

            # Auto mode: predict as soon as buffer is full (continuous)
            if args.auto and not args.use_sliding_window and len(buffer) == args.frames and ready_for_next and pending_future is None:
                pending_future = executor.submit(run_prediction_job, model, list(buffer), device, args.top_k)
                pending_source = 'auto'
                buffer.clear()
                last_good_mouth = None
                ready_for_next = False  # avoid re-trigger until buffer refills

            # Reset readiness once buffer has space again in auto mode
            if args.auto and not args.use_sliding_window and len(buffer) < args.frames:
                ready_for_next = True
    finally:
        cap.release()
        if args.display:
            cv2.destroyAllWindows()
        executor.shutdown(wait=False)
        # Close mediapipe resources if used
        try:
            if detector is not None:
                detector.close()
        except Exception:
            pass


if __name__ == '__main__':
    main()
