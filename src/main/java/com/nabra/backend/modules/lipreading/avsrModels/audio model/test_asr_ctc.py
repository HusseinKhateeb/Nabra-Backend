import argparse
import importlib
import os
import time
from pathlib import Path

import numpy as np
import torch
import torchaudio
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor


os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
os.environ.setdefault("HF_HUB_OFFLINE", "1")


def load_audio(audio_path: str):
    try:
        return torchaudio.load(audio_path)
    except Exception as exc:
        error_text = str(exc).lower()
        if "torchcodec" not in error_text and "libtorchcodec" not in error_text:
            raise
        try:
            pydub_module = importlib.import_module("pydub")
            audio_segment_cls = getattr(pydub_module, "AudioSegment")

            try:
                imageio_ffmpeg = importlib.import_module("imageio_ffmpeg")
                ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
                audio_segment_cls.converter = ffmpeg_exe
                audio_segment_cls.ffprobe = ffmpeg_exe.replace("ffmpeg", "ffprobe")
            except Exception:
                pass
        except Exception as fallback_exc:
            raise ImportError(
                "Failed to load audio. Install one of:\n"
                "1) pip install torchcodec\n"
                "2) pip install pydub imageio-ffmpeg"
            ) from fallback_exc

        audio = audio_segment_cls.from_file(audio_path)
        sr = audio.frame_rate
        channels = audio.channels
        samples = np.array(audio.get_array_of_samples(), dtype=np.float32)

        max_val = float(1 << (8 * audio.sample_width - 1))
        samples = samples / max_val

        if channels > 1:
            samples = samples.reshape(-1, channels).T
        else:
            samples = samples.reshape(1, -1)

        waveform = torch.from_numpy(samples)
        return waveform, sr


def load_asr(model_id: str):
    device = "cuda" if torch.cuda.is_available() else "cpu"

    try:
        processor = Wav2Vec2Processor.from_pretrained(model_id, local_files_only=True)
        model = Wav2Vec2ForCTC.from_pretrained(model_id, local_files_only=True).to(device)
    except Exception as exc:
        raise RuntimeError(
            "Offline mode is enabled, but model files were not found in local cache. "
            "Connect to internet once to download the model, then run again offline."
        ) from exc
    model.eval()

    return processor, model, device


def transcribe_waveform(
    waveform: torch.Tensor,
    sr: int,
    processor,
    model,
    device: str,
) -> str:
    if waveform.dim() == 1:
        waveform = waveform.unsqueeze(0)

    if waveform.size(0) > 1:
        waveform = waveform.mean(dim=0, keepdim=True)

    target_sr = processor.feature_extractor.sampling_rate
    if sr != target_sr:
        waveform = torchaudio.functional.resample(waveform, sr, target_sr)
        sr = target_sr

    inputs = processor(
        waveform.squeeze().numpy(),
        sampling_rate=sr,
        return_tensors="pt",
        padding=True,
    )
    input_values = inputs.input_values.to(device)

    with torch.no_grad():
        logits = model(input_values).logits

    pred_ids = torch.argmax(logits, dim=-1)
    return processor.batch_decode(pred_ids)[0]


def transcribe(audio_path: str, model_id: str) -> str:
    processor, model, device = load_asr(model_id)
    waveform, sr = load_audio(audio_path)
    return transcribe_waveform(waveform, sr, processor, model, device)


def buckwalter_to_arabic(text: str) -> str:
    mapping = {
        "|": "آ",
        ">": "أ",
        "&": "ؤ",
        "<": "إ",
        "}": "ئ",
        "'": "ء",
        "A": "ا",
        "b": "ب",
        "p": "ة",
        "t": "ت",
        "v": "ث",
        "j": "ج",
        "H": "ح",
        "x": "خ",
        "d": "د",
        "*": "ذ",
        "r": "ر",
        "z": "ز",
        "s": "س",
        "$": "ش",
        "S": "ص",
        "D": "ض",
        "T": "ط",
        "Z": "ظ",
        "E": "ع",
        "g": "غ",
        "_": "ـ",
        "f": "ف",
        "q": "ق",
        "k": "ك",
        "l": "ل",
        "m": "م",
        "n": "ن",
        "h": "ه",
        "w": "و",
        "Y": "ى",
        "y": "ي",
        "F": "ً",
        "N": "ٌ",
        "K": "ٍ",
        "a": "َ",
        "u": "ُ",
        "i": "ِ",
        "~": "ّ",
        "o": "ْ",
        "`": "ٰ",
        "{": "ٱ",
    }
    return "".join(mapping.get(ch, ch) for ch in text)


def remove_arabic_diacritics(text: str) -> str:
    diacritics_and_marks = "\u064b\u064c\u064d\u064e\u064f\u0650\u0651\u0652\u0670\u0640"
    return text.translate({ord(ch): None for ch in diacritics_and_marks})


def run_realtime_mode(model_id: str, output_file: Path, duration: float = 2.0) -> None:
    try:
        sd = importlib.import_module("sounddevice")
    except Exception as exc:
        raise ImportError("Realtime mode needs sounddevice. Install with: pip install sounddevice") from exc

    try:
        import msvcrt
    except Exception as exc:
        raise RuntimeError("Realtime key mode is supported on Windows console only.") from exc

    processor, model, device = load_asr(model_id)
    sample_rate = int(processor.feature_extractor.sampling_rate)

    print("Realtime mode started.")
    print(f"Press 'p' to record {duration:g} seconds and transcribe.")
    print("Press 'q' to quit.")

    while True:
        if msvcrt.kbhit():
            key = msvcrt.getwch().lower()

            if key == "q":
                print("Exiting realtime mode.")
                break

            if key == "p":
                print(f"Recording {duration:g} seconds...")
                recording = sd.rec(
                    int(duration * sample_rate),
                    samplerate=sample_rate,
                    channels=1,
                    dtype="float32",
                )
                sd.wait()

                waveform = torch.from_numpy(recording.T)
                raw_text = transcribe_waveform(waveform, sample_rate, processor, model, device)
                arabic_text = remove_arabic_diacritics(buckwalter_to_arabic(raw_text))

                with output_file.open("a", encoding="utf-8") as f:
                    f.write(arabic_text + "\n")

                print("ASR (raw):", raw_text)
                print("ASR (Arabic):", arabic_text)
                print(f"Saved Arabic transcription to: {output_file}")

        time.sleep(0.03)


def main() -> None:
    parser = argparse.ArgumentParser(description="Test a Wav2Vec2 CTC ASR model on an audio file or microphone")
    parser.add_argument(
        "audio_path",
        nargs="?",
        default="test3.m4a",
        help="Path to input audio file (default: test3.m4a)",
    )
    parser.add_argument(
        "--model",
        default="elgeish/wav2vec2-large-xlsr-53-levantine-arabic",
        help="Hugging Face model ID",
    )
    parser.add_argument(
        "--realtime",
        action="store_true",
        help="Realtime mode: press p to record from mic, q to quit",
    )
    parser.add_argument(
        "--duration",
        type=float,
        default=2.0,
        help="Realtime recording duration in seconds (default: 2.0)",
    )
    parser.add_argument(
        "--output",
        default="realtime_result.txt",
        help="Output txt file in realtime mode (default: realtime_result.txt)",
    )
    args = parser.parse_args()

    if args.realtime:
        run_realtime_mode(args.model, Path(args.output), args.duration)
        return

    audio_file = Path(args.audio_path)
    if not audio_file.exists():
        raise FileNotFoundError(f"Audio file not found: {audio_file}")

    text = transcribe(str(audio_file), args.model)
    arabic_text = remove_arabic_diacritics(buckwalter_to_arabic(text))

    output_file = audio_file.with_suffix(".txt")
    output_file.write_text(arabic_text + "\n", encoding="utf-8")

    print("ASR (raw):", text)
    print("ASR (Arabic):", arabic_text)
    print(f"Saved Arabic transcription to: {output_file}")


if __name__ == "__main__":
    main()