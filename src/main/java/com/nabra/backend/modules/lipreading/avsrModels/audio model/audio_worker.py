import io
import json
import sys
from test_asr_ctc import load_asr, load_audio, transcribe_waveform, normalize_asr_text

# Hardcoded model ID (change if needed)
MODEL_ID = "elgeish/wav2vec2-large-xlsr-53-levantine-arabic"

def load_model_once():
    processor, model, device = load_asr(MODEL_ID)
    return model, processor, device

def run_audio_inference(audio_path, model, processor, device):
    waveform, sr = load_audio(audio_path)
    raw_result = transcribe_waveform(waveform, sr, processor, model, device)
    arabic_result = normalize_asr_text(raw_result)
    return arabic_result

def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", line_buffering=True)
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")
    # Load model ONCE here
    model, processor, device = load_model_once()

    for line in sys.stdin:
        payload = line.strip()
        if not payload:
            continue
        try:
            request = json.loads(payload)
            audio_path = request.get("audioPath")
            if not audio_path:
                print(json.dumps({"ok": False, "error": "audioPath is required"}, ensure_ascii=False), flush=True)
                continue
            result = run_audio_inference(audio_path, model, processor, device)
            # Ensure result is a string and encodeable as UTF-8
            if not isinstance(result, str):
                result = str(result)
            try:
                result.encode('utf-8')
            except Exception:
                result = result.encode('utf-8', errors='replace').decode('utf-8')
            print(json.dumps({"result": result}, ensure_ascii=False), flush=True)
        except Exception as ex:
            print(json.dumps({"ok": False, "error": f"{type(ex).__name__}: {ex}"}, ensure_ascii=False), flush=True)

if __name__ == "__main__":
    import sys
    print("[DEBUG] Python executable:", sys.executable)
    print("[DEBUG] Python version:", sys.version)
    main()