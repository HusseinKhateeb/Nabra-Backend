"""
Test with Realtek microphone (device 3)
"""
import sounddevice as sd
import numpy as np

device_id = 3  # Microphone Array (Realtek(R) Audio)

print("="*60)
print(f"Testing Realtek Microphone (Device {device_id})")
print("="*60)

# Get device info
device_info = sd.query_devices(device_id)
print(f"\nDevice: {device_info['name']}")
print(f"Sample Rate: {device_info['default_samplerate']} Hz")

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
    device=device_id
)
sd.wait()

print("\nRecording complete!")

# Analyze
audio_data = recording.flatten()
max_amplitude = np.max(np.abs(audio_data))
rms = np.sqrt(np.mean(audio_data**2))

print(f"\nAudio Analysis:")
print(f"  Max amplitude: {max_amplitude:.4f} (should be > 0.01)")
print(f"  RMS level: {rms:.4f} (should be > 0.005)")

if max_amplitude < 0.01:
    print("\n⚠️  Still too quiet with Realtek mic")
    print("  → You MUST increase microphone volume in Windows!")
else:
    print("\n✅ Realtek microphone works better!")
    print(f"  → Use device={device_id} in the script")
