"""
Download YOLOv5n TensorFlow Lite model for ARound-Me/Aurie app
This script downloads a pre-trained YOLOv5n model and labels file
"""

import os
import requests
from pathlib import Path

# Output directories
ASSETS_DIR = Path(__file__).parent.parent.parent / "app" / "src" / "main" / "assets"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

# YOLOv5n TFLite model URL (lightweight, optimized for mobile)
MODEL_URL = "https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5n-fp16.tflite"
MODEL_PATH = ASSETS_DIR / "yolov5n.tflite"

# COCO labels
LABELS_PATH = ASSETS_DIR / "labels.txt"

# COCO class names (80 classes)
COCO_LABELS = [
    "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
    "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
    "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
    "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
    "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
    "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
    "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
    "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
    "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
    "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
    "toothbrush"
]


def download_file(url: str, output_path: Path, description: str):
    """Download a file with progress reporting"""
    print(f"📥 Downloading {description}...")
    print(f"   URL: {url}")
    print(f"   Output: {output_path}")
    
    try:
        response = requests.get(url, stream=True, timeout=30)
        response.raise_for_status()
        
        total_size = int(response.headers.get('content-length', 0))
        downloaded = 0
        
        with open(output_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"\r   Progress: {percent:.1f}% ({downloaded}/{total_size} bytes)", end='')
        
        print(f"\n✅ Downloaded {description} successfully!")
        print(f"   Size: {os.path.getsize(output_path) / (1024*1024):.2f} MB\n")
        return True
        
    except Exception as e:
        print(f"\n❌ Error downloading {description}: {e}\n")
        return False


def create_labels_file():
    """Create labels.txt file with COCO class names"""
    print("📝 Creating labels file...")
    try:
        with open(LABELS_PATH, 'w', encoding='utf-8') as f:
            for label in COCO_LABELS:
                f.write(f"{label}\n")
        print(f"✅ Created labels file with {len(COCO_LABELS)} classes\n")
        return True
    except Exception as e:
        print(f"❌ Error creating labels file: {e}\n")
        return False


def download_alternative_model():
    """Download alternative YOLOv5 model if main download fails"""
    print("🔄 Trying alternative model source...")
    
    # Alternative: Download from TensorFlow Hub or other sources
    alt_urls = [
        "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/latest/efficientdet_lite0.tflite",
    ]
    
    for i, url in enumerate(alt_urls):
        alt_path = ASSETS_DIR / f"model_alt_{i}.tflite"
        if download_file(url, alt_path, f"Alternative model {i+1}"):
            print(f"✅ You can use this alternative model: {alt_path.name}")
            return True
    
    return False


def main():
    print("=" * 60)
    print("YOLOv5 Model Downloader for Aurie (ARound-Me)")
    print("=" * 60)
    print()
    
    # Create labels file first (always works)
    create_labels_file()
    
    # Try to download YOLOv5n model
    if MODEL_PATH.exists():
        print(f"⚠️  Model already exists: {MODEL_PATH}")
        overwrite = input("   Overwrite? (y/n): ").lower().strip()
        if overwrite != 'y':
            print("✅ Using existing model\n")
            print_summary()
            return
    
    success = download_file(MODEL_URL, MODEL_PATH, "YOLOv5n TFLite model")
    
    if not success:
        print("⚠️  Main download failed. Trying alternatives...")
        download_alternative_model()
    
    print_summary()


def print_summary():
    """Print summary of downloaded files"""
    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"\nAssets directory: {ASSETS_DIR}\n")
    
    files = [
        ("yolov5n.tflite", "YOLOv5n model (recommended)"),
        ("labels.txt", "COCO class labels"),
        ("model_alt_0.tflite", "Alternative model (if main failed)"),
    ]
    
    for filename, description in files:
        filepath = ASSETS_DIR / filename
        if filepath.exists():
            size_mb = os.path.getsize(filepath) / (1024 * 1024)
            print(f"✅ {filename:25} - {description} ({size_mb:.2f} MB)")
        else:
            print(f"❌ {filename:25} - {description} (not found)")
    
    print("\n" + "=" * 60)
    print("NEXT STEPS:")
    print("=" * 60)
    print("1. Build the Android app in Android Studio")
    print("2. Run on a physical device (required for camera)")
    print("3. Grant camera and microphone permissions")
    print("4. Say 'Hey Aurie' to interact with the assistant")
    print("\nVoice Commands:")
    print("  • 'Switch to continuous mode' - Continuous environment description")
    print("  • 'Switch to normal mode' - Alert-only mode")
    print("  • 'What's around me?' - Describe surroundings")
    print("  • 'Read signs' - OCR text recognition")
    print("  • 'Turn flashlight on/off' - Control flashlight")
    print("=" * 60)
    print()


if __name__ == "__main__":
    main()
