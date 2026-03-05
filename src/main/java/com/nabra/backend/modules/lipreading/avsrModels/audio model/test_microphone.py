"""
Test microphone to diagnose audio recording issues
"""
import sounddevice as sd
import numpy as np

print("="*60)
print("MICROPHONE DIAGNOSTIC TEST")
print("="*60)

# List all audio devices
print("\nAvailable Audio Devices:")
print(sd.query_devices())

# Get default input device
default_input = sd.query_devices(kind='input')
print(f"\nDefault Input Device: {default_input['name']}")
print(f"Sample Rate: {default_input['default_samplerate']} Hz")
print(f"Channels: {default_input['max_input_channels']}")

# Record 3 seconds of audio
duration = 3.0
sample_rate = 16000

print(f"\n{'='*60}")
print(">>> SPEAK NOW FOR 3 SECONDS! <<<")
print(f"{'='*60}")

recording = sd.rec(
    int(duration * sample_rate),
    samplerate=sample_rate,
    channels=1,
    dtype="float32",
)
sd.wait()

print("\nRecording complete!")

# Analyze the recording
audio_data = recording.flatten()
max_amplitude = np.max(np.abs(audio_data))
rms = np.sqrt(np.mean(audio_data**2))

print(f"\nAudio Analysis:")
print(f"  Max amplitude: {max_amplitude:.4f} (should be > 0.01 for speech)")
print(f"  RMS level: {rms:.4f} (should be > 0.005 for speech)")
print(f"  Total samples: {len(audio_data)}")

if max_amplitude < 0.01:
    print("\n⚠️  WARNING: Audio is very quiet!")
    print("  Solutions:")
    print("  1. Check Windows microphone volume (increase to 100%)")
    print("  2. Speak louder and closer to the microphone")
    print("  3. Try a different microphone")
    print("  4. Check if microphone is muted")
elif max_amplitude > 0.9:
    print("\n⚠️  WARNING: Audio might be clipping!")
    print("  Solution: Reduce microphone volume")
else:
    print("\n✅ Audio levels look good!")

print(f"\n{'='*60}")
