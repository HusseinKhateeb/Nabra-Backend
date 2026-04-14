import argparse
import json
import sys
import torch

from avsr_batch_fusion import (
    CHECKPOINT_PATH,
    WORD_MAP_PATH,
    extract_mouth_frames,
    get_lip_model,
    load_word_map,
    predict_lip,
)


def run_lip_only(video_path: str, top_k: int = 5, frame_count: int = 25):
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    _, idx_to_word = load_word_map(WORD_MAP_PATH)
    model = get_lip_model(len(idx_to_word))
    ckpt = torch.load(CHECKPOINT_PATH, map_location=device)
    if isinstance(ckpt, dict) and 'model_state_dict' in ckpt:
        model.load_state_dict(ckpt['model_state_dict'])
    else:
        model.load_state_dict(ckpt)
    model = model.to(device).eval()

    frames, detected_face_frames, face_detection_enabled = extract_mouth_frames(video_path, frame_count=frame_count)
    if not frames:
        raise RuntimeError("No frames could be extracted from the video")
    if face_detection_enabled and detected_face_frames == 0:
        raise RuntimeError("No face detected in video frames")

    # Always use top_k=40 for prediction
    best_word, best_conf, top_predictions = predict_lip(model, frames, device, idx_to_word, top_k=40)
    return {
        "bestPrediction": {
            "word": best_word,
            "confidence": best_conf,
        },
        "topPredictions": [
            {"word": word, "confidence": confidence}
            for word, confidence in top_predictions
        ],
    }


def main():
    parser = argparse.ArgumentParser(description="Lip-only file inference for Nabra 3D ResNet-18")
    parser.add_argument("--video", required=True, help="Path to input video")
    parser.add_argument("--top-k", type=int, default=5, help="Top-K predictions to return")
    parser.add_argument("--frame-count", type=int, default=25, help="Temporal window size")
    parser.add_argument("--use-mediapipe", action="store_true", help="Compatibility flag")
    parser.add_argument("--json-only", action="store_true", help="Print JSON output only")
    args = parser.parse_args()

    result = run_lip_only(args.video, top_k=args.top_k, frame_count=args.frame_count)
    output = json.dumps(result, ensure_ascii=False)
    if args.json_only:
        print(output)
    else:
        print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as ex:
        print(json.dumps({"error": f"{type(ex).__name__}: {ex}"}, ensure_ascii=False))
        sys.exit(1)
