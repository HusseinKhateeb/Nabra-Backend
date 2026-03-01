
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
# Update to absolute path for .venv python
ASR_VENV_PYTHON = 'D:/Graduation Extra/Nabra Workspace/.venv/Scripts/python.exe'

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
_FUSION_EXECUTOR = ThreadPoolExecutor(max_workers=2)
DEFAULT_FRAME_COUNT = max(8, int(os.getenv("AVSR_FRAME_COUNT", "25")))


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
    frames = []
    detector = get_face_detector()

    while len(frames) < frame_count:
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
        if mouth is None:
            mouth = frame
        mouth_resized = cv2.resize(mouth, (160, 100), interpolation=cv2.INTER_CUBIC)
        resized = cv2.resize(mouth_resized, (img_size, img_size), interpolation=cv2.INTER_CUBIC)
        rgb = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
        arr = rgb.astype(np.float32) / 255.0
        arr = np.transpose(arr, (2, 0, 1))
        frames.append(arr)
    cap.release()
    return frames

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
    try:
        result = subprocess.run([
            ASR_VENV_PYTHON,
            ASR_SCRIPT_PATH,
            str(audio_path)
        ], capture_output=True, text=True, encoding='utf-8', timeout=30)
        logging.debug(f"ASR stdout: {result.stdout}")
        logging.debug(f"ASR stderr: {result.stderr}")
        logging.debug(f"ASR returncode: {result.returncode}")
        return result.stdout.strip()
    except Exception as e:
        logging.error(f"ASR subprocess error: {e}")
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
    frames = extract_mouth_frames(video_path, frame_count=effective_frame_count)
    lip_word, lip_conf, lip_top = predict_lip(_CACHED_MODEL, frames, _CACHED_DEVICE, _CACHED_IDX_TO_WORD)
    asr_output = asr_future.result(timeout=35)
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