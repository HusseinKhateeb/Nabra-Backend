import whisper
from flask import Flask, request, jsonify

app = Flask(__name__)
model = whisper.load_model("base")  # You can use "small", "medium", "large" for better accuracy

@app.route("/transcribe", methods=["POST"])
def transcribe():
    data = request.get_json()
    audio_url = data.get("audioUrl")
    language = data.get("preferredLanguage", "ar")
    if not audio_url:
        return jsonify({"error": "audioUrl required"}), 400


    # Download audio file and ensure file is closed before transcription (Windows fix)
    import requests, tempfile, os
    with tempfile.NamedTemporaryFile(suffix=".mp3", delete=False) as tmp:
        r = requests.get(audio_url)
        tmp.write(r.content)
        tmp_path = tmp.name

    try:
        result = model.transcribe(tmp_path, language=language, task="transcribe")
    finally:
        os.remove(tmp_path)
    return jsonify({"text": result["text"]})

if __name__ == "__main__":
    import sys
    print("[DEBUG] Python executable:", sys.executable)
    print("[DEBUG] Python version:", sys.version)
    app.run(host="0.0.0.0", port=5000)