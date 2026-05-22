import argparse
import importlib
import os
import re
import sys
import time
from pathlib import Path

# Force UTF-8 encoding for stdout to avoid UnicodeEncodeError on Windows
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import numpy as np
import torch

try:
    import whisper
except Exception as exc:
    whisper = None
    _WHISPER_IMPORT_ERROR = exc
else:
    _WHISPER_IMPORT_ERROR = None

try:
    import torchaudio
except Exception:
    torchaudio = None


os.environ.setdefault("TRANSFORMERS_OFFLINE", "0")
os.environ.setdefault("HF_HUB_OFFLINE", "0")

# Force Arabic-only transcription for both upload-audio and AVSR fusion endpoints.
WHISPER_LANGUAGE = "ar"
WHISPER_TEMPERATURE = float(os.getenv("AVSR_WHISPER_TEMPERATURE", "0"))
WHISPER_BEAM_SIZE = int(os.getenv("AVSR_WHISPER_BEAM_SIZE", "1"))
WHISPER_BEST_OF = int(os.getenv("AVSR_WHISPER_BEST_OF", "1"))
WHISPER_CONDITION_ON_PREVIOUS_TEXT = os.getenv("AVSR_WHISPER_CONDITION_ON_PREVIOUS_TEXT", "0").strip().lower() in {"1", "true", "yes"}


def load_audio(audio_path: str):
    try:
        import soundfile as sf
        samples, sr = sf.read(audio_path, always_2d=True, dtype="float32")
        waveform = torch.from_numpy(samples.T)
        return waveform, int(sr)
    except Exception:
        pass

    try:
        import wave
        with wave.open(audio_path, "rb") as wav_file:
            sr = wav_file.getframerate()
            channels = wav_file.getnchannels()
            sample_width = wav_file.getsampwidth()
            nframes = wav_file.getnframes()
            raw = wav_file.readframes(nframes)

        if sample_width == 1:
            np_audio = np.frombuffer(raw, dtype=np.uint8).astype(np.float32)
            np_audio = (np_audio - 128.0) / 128.0
        elif sample_width == 2:
            np_audio = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768.0
        elif sample_width == 4:
            np_audio = np.frombuffer(raw, dtype=np.int32).astype(np.float32) / 2147483648.0
        else:
            raise ValueError(f"Unsupported WAV sample width: {sample_width}")

        if channels > 1:
            np_audio = np_audio.reshape(-1, channels)
            waveform = torch.from_numpy(np_audio.T)
        else:
            waveform = torch.from_numpy(np_audio.reshape(1, -1))
        return waveform, int(sr)
    except Exception:
        pass

    if torchaudio is None:
        raise ImportError(
            "Failed to load audio. Install one of:\n"
            "1) pip install soundfile\n"
            "2) pip install torchcodec\n"
            "3) pip install pydub imageio-ffmpeg"
        )

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
    if whisper is None:
        raise RuntimeError(
            "Whisper is not installed. Install it with: pip install openai-whisper"
        ) from _WHISPER_IMPORT_ERROR

    device = "cuda" if torch.cuda.is_available() else "cpu"
    model_name = (model_id or os.getenv("AVSR_WHISPER_MODEL", "base")).strip() or "base"

    try:
        model = whisper.load_model(model_name, device=device)
    except Exception as exc:
        raise RuntimeError(
            f"Failed to load Whisper model '{model_name}'. Check that the model is available and FFmpeg is installed."
        ) from exc

    return None, model, device


def transcribe_waveform(
    waveform: torch.Tensor,
    sr: int,
    processor,
    model,
    device: str,
) -> str:
    if whisper is None:
        raise RuntimeError("Whisper is not installed. Install it with: pip install openai-whisper")

    if waveform.dim() == 1:
        waveform = waveform.unsqueeze(0)

    if waveform.size(0) > 1:
        waveform = waveform.mean(dim=0, keepdim=True)

    target_sr = 16000
    if sr != target_sr:
        if torchaudio is not None:
            waveform = torchaudio.functional.resample(waveform, sr, target_sr)
        else:
            original_len = waveform.size(1)
            target_len = max(1, int(round(original_len * float(target_sr) / float(sr))))
            src_idx = np.linspace(0.0, 1.0, num=original_len, endpoint=True)
            dst_idx = np.linspace(0.0, 1.0, num=target_len, endpoint=True)
            resampled = np.interp(dst_idx, src_idx, waveform.squeeze(0).cpu().numpy()).astype(np.float32)
            waveform = torch.from_numpy(resampled).unsqueeze(0)

    audio = waveform.squeeze().detach().cpu().numpy().astype(np.float32)
    if np.max(np.abs(audio)) > 1.0:
        audio = np.clip(audio / 32768.0, -1.0, 1.0)

    decode_options = {
        "language": WHISPER_LANGUAGE,
        "task": "transcribe",
        "fp16": device == "cuda",
        "verbose": False,
        "without_timestamps": True,
        "temperature": WHISPER_TEMPERATURE,
        "condition_on_previous_text": WHISPER_CONDITION_ON_PREVIOUS_TEXT,
    }
    if WHISPER_TEMPERATURE == 0:
        decode_options["beam_size"] = WHISPER_BEAM_SIZE
        decode_options["best_of"] = WHISPER_BEST_OF

    result = model.transcribe(audio, **decode_options)
    return (result.get("text") or "").strip()


def transcribe(audio_path: str, model_id: str) -> str:
    _, model, device = load_asr(model_id)
    waveform, sr = load_audio(audio_path)
    return transcribe_waveform(waveform, sr, None, model, device)


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


def clean_recognized_text(text: str) -> str:
    if not text:
        return ""
    cleaned = " ".join(text.strip().split())
    cleaned = cleaned.lstrip("/\\|_*-:;.,!؟،[]{}()\"'`")
    cleaned = re.sub(r"[^\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF0-9\s]", " ", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def normalize_asr_text(raw_text: str) -> str:
    if not raw_text:
        return ""
    cleaned = clean_recognized_text(remove_arabic_diacritics(raw_text))
    if cleaned:
        return cleaned
    return raw_text.strip()


def run_realtime_mode(model_id: str, output_file: Path, duration: float = 2.0) -> None:
    try:
        sd = importlib.import_module("sounddevice")
    except Exception as exc:
        raise ImportError("Realtime mode needs sounddevice. Install with: pip install sounddevice") from exc

    try:
        import msvcrt
    except Exception as exc:
        raise RuntimeError("Realtime key mode is supported on Windows console only.") from exc

    _, model, device = load_asr(model_id)
    sample_rate = 16000

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
                raw_text = transcribe_waveform(waveform, sample_rate, None, model, device)
                arabic_text = normalize_asr_text(raw_text)

                with output_file.open("a", encoding="utf-8") as f:
                    f.write(arabic_text + "\n")

                print("ASR (raw):", raw_text)
                print("ASR (Arabic):", arabic_text)
                print(f"Saved Arabic transcription to: {output_file}")

        time.sleep(0.03)


def run_mic_once(model_id: str, output_file: Path, duration: float = 2.0, device_id=None) -> str:
    try:
        sd = importlib.import_module("sounddevice")
    except Exception as exc:
        raise ImportError("Mic-once mode needs sounddevice. Install with: pip install sounddevice") from exc

    sample_rate = 16000

    # Use specific device or default
    if device_id is not None:
        device_info = sd.query_devices(device_id)
        print(f"Using microphone: {device_info['name']}")
    
    print(f"Recording {duration:g} seconds...")
    recording = sd.rec(
        int(duration * sample_rate),
        samplerate=sample_rate,
        channels=1,
        dtype="float32",
        device=device_id,
    )
    sd.wait()

    _, model, device = load_asr(model_id)

    waveform = torch.from_numpy(recording.T)
    raw_text = transcribe_waveform(waveform, sample_rate, None, model, device)
    arabic_text = normalize_asr_text(raw_text)

    with output_file.open("w", encoding="utf-8") as f:
        f.write(arabic_text + "\n")

    print("ASR (raw):", raw_text)
    print("ASR (Arabic):", arabic_text)
    print(f"Saved Arabic transcription to: {output_file}")
    return arabic_text


def run_mic_service(model_id: str, duration: float = 2.0, device_id=None) -> None:
    try:
        sd = importlib.import_module("sounddevice")
    except Exception as exc:
        raise ImportError("Mic service mode needs sounddevice. Install with: pip install sounddevice") from exc

    sample_rate = 16000
    _, model, device = load_asr(model_id)

    while True:
        line = sys.stdin.readline()
        if not line:
            break

        parts = line.rstrip("\n").split("\t", 2)
        command = parts[0].strip().upper() if parts else ""

        if command == "QUIT":
            break

        if command != "REC":
            continue

        req_duration = duration
        if len(parts) > 1 and parts[1]:
            try:
                req_duration = float(parts[1])
            except Exception:
                req_duration = duration

        output_file = Path("realtime_result.txt")
        if len(parts) > 2 and parts[2]:
            output_file = Path(parts[2])

        recording = sd.rec(
            int(req_duration * sample_rate),
            samplerate=sample_rate,
            channels=1,
            dtype="float32",
            device=device_id,
        )
        sd.wait()

        waveform = torch.from_numpy(recording.T)
        raw_text = transcribe_waveform(waveform, sample_rate, None, model, device)
        arabic_text = normalize_asr_text(raw_text)

        output_file.parent.mkdir(parents=True, exist_ok=True)
        with output_file.open("w", encoding="utf-8") as f:
            f.write(arabic_text + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="Test a Whisper ASR model on an audio file or microphone")
    parser.add_argument(
        "audio_path",
        nargs="?",
        default="test3.m4a",
        help="Path to input audio file (default: test3.m4a)",
    )
    parser.add_argument(
        "--model",
        default=os.getenv("AVSR_WHISPER_MODEL", "base"),
        help="Whisper model name, e.g. tiny, base, small, medium, large-v3",
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
    parser.add_argument(
        "--mic-once",
        action="store_true",
        help="Record one clip from mic and transcribe once (non-interactive)",
    )
    parser.add_argument(
        "--device",
        type=int,
        default=None,
        help="Audio device ID (use 3 for Realtek mic to bypass SteelSeries, None for default)",
    )
    parser.add_argument(
        "--mic-service",
        action="store_true",
        help="Persistent mic service mode (reads REC/QUIT commands from stdin)",
    )
    args = parser.parse_args()

    if args.mic_service:
        run_mic_service(args.model, args.duration, device_id=args.device)
        return

    if args.mic_once:
        run_mic_once(args.model, Path(args.output), args.duration, device_id=args.device)
        return

    if args.realtime:
        run_realtime_mode(args.model, Path(args.output), args.duration)
        return

    audio_file = Path(args.audio_path)
    if not audio_file.exists():
        raise FileNotFoundError(f"Audio file not found: {audio_file}")

    text = transcribe(str(audio_file), args.model)
    arabic_text = normalize_asr_text(text)

    output_file = audio_file.with_suffix(".txt")
    output_file.write_text(arabic_text + "\n", encoding="utf-8")

    print("ASR (raw):", text)
    print("ASR (Arabic):", arabic_text)
    print(f"Saved Arabic transcription to: {output_file}")


if __name__ == "__main__":
    main()