"""
Persistent AVSR fusion worker.
Loads heavy models once, then serves requests from stdin as JSON lines:
{"audioPath":"...", "videoPath":"..."}

Writes one JSON line per request to stdout:
{"ok": true, "rawOutput": "..."}
or
{"ok": false, "error": "..."}
"""

import io
import json
import sys

from avsr_batch_fusion import run_fusion


def main():
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", line_buffering=True)
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")

    for line in sys.stdin:
        payload = line.strip()
        if not payload:
            continue

        try:
            request = json.loads(payload)
            audio_path = request.get("audioPath")
            video_path = request.get("videoPath")

            if not audio_path or not video_path:
                print(json.dumps({"ok": False, "error": "audioPath and videoPath are required"}, ensure_ascii=False), flush=True)
                continue

            result = run_fusion(audio_path, video_path)
            raw_output = json.dumps(result, ensure_ascii=False, indent=2)
            print(json.dumps({"ok": True, "rawOutput": raw_output}, ensure_ascii=False), flush=True)
        except Exception as ex:
            print(json.dumps({"ok": False, "error": f"{type(ex).__name__}: {ex}"}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
