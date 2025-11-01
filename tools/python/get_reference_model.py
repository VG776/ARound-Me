import argparse
import os
import sys
import urllib.request

DEFAULT_URL = (
    "https://storage.googleapis.com/tfhub-lite-models/"
    "tensorflow/lite-model/efficientdet/lite0/detection/metadata/2.tflite"
)


def download(url: str, out_path: str):
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    print(f"Downloading from: {url}")
    print(f"Saving to: {out_path}")
    try:
        # Some hosts (incl. GCS behind tfhub) may require a user-agent header.
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req) as resp, open(out_path, "wb") as f:
            f.write(resp.read())
    except Exception as e:
        print("ERROR: download failed:", e)
        sys.exit(1)
    print("Done.")


def main():
    parser = argparse.ArgumentParser(description="Download a reference EfficientDet Lite0 TFLite model.")
    parser.add_argument("--url", default=DEFAULT_URL, help="Model URL")
    parser.add_argument(
        "--out",
        default=os.path.join("..", "..", "app", "src", "main", "assets", "model_ref_efficientdet_lite0.tflite"),
        help="Output path for the downloaded model",
    )
    args = parser.parse_args()

    out_abs = os.path.abspath(args.out)
    download(args.url, out_abs)


if __name__ == "__main__":
    main()
