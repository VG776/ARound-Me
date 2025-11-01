# Aurie - ARound Me Enhanced

**A real-time, voice-interactive assistive vision app for visually impaired users**

Aurie is an intelligent companion that helps visually impaired users navigate their environment safely using:
- 🎯 Real-time object detection with YOLOv5
- 🎤 Voice-controlled interactive assistant
- 📖 OCR for reading signs and text
- 💡 Automatic flashlight control
- 📳 Vibration alerts for obstacles
- 🗣️ Natural text-to-speech feedback

---

## ✨ Features

### 1. **Real-Time Object Detection**
- YOLOv5n TensorFlow Lite model for low-latency detection
- Processes frames asynchronously (no UI blocking)
- Safe distance estimation with 4 levels: DANGER, WARNING, SAFE, FAR
- Detects 80 COCO object classes

### 2. **Voice-Controlled Assistant (Aurie)**
- Always-listening speech recognition
- Natural language command processing
- Empathetic, calm voice responses
- Automatic speech throttling (no nagging)

#### Voice Commands:
- **"Switch to continuous mode"** - Aurie continuously describes surroundings
- **"Switch to normal mode"** - Only alerts for obstacles
- **"What's around me?"** - Describe current environment
- **"Read signs"** - OCR text recognition
- **"Turn flashlight on/off"** - Control camera flash
- **"Hello Aurie"** - Greeting/test command

### 3. **Two Operating Modes**

#### Normal Mode (Default)
- Announces only DANGER and WARNING level objects
- Vibrates before speaking
- Prioritizes urgent alerts

#### Continuous Mode
- Continuously describes environment
- Speaks top 3 detected objects
- Updates as user moves

### 4. **Smart Vibration Alerts**
- **Danger:** Double buzz pattern (200ms + 200ms)
- **Warning:** Single buzz (150ms)
- 2-second cooldown to prevent spam

### 5. **OCR Text Recognition**
- ML Kit Text Recognition
- Detects arrow symbols: ← → ↑ ↓
- Reads signboards, directions, labels
- Voice command: "Read signs"

### 6. **Automatic Flashlight Control**
- Auto-enables in low light (< 50 brightness)
- Manual toggle via voice command
- Auto-disables in bright conditions

---

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio (latest version)
- Physical Android device (API 21+)
- Python 3.7+ (for downloading model)

### Step 1: Download YOLOv5 Model

Run the Python script to download the model and labels:

```bash
cd tools/python
python download_yolo_model.py
```

This will create:
- `app/src/main/assets/yolov5n.tflite` (YOLOv5n model)
- `app/src/main/assets/labels.txt` (COCO class labels)

### Step 2: Build the App

1. Open the project in Android Studio
2. Sync Gradle files (should auto-sync)
3. Build the app: **Build > Make Project**
4. Connect your Android device
5. Run the app: **Run > Run 'app'**

### Step 3: Grant Permissions

When the app launches, grant:
- ✅ Camera permission (required)
- ✅ Microphone permission (required)

---

## 📱 Usage

### First Launch
1. App starts with "Hello, I'm Aurie. I'm here to guide you safely."
2. Camera preview activates
3. Aurie begins listening for commands
4. Object detection starts automatically

### Normal Mode Usage
- Walk normally
- Aurie announces obstacles in your path
- Vibration alerts before speech
- Only urgent objects are announced

### Continuous Mode
- Say: **"Switch to continuous mode"**
- Aurie continuously describes surroundings
- Lists nearby objects with positions
- Switch back: **"Switch to normal mode"**

### Reading Signs
- Point camera at a sign
- Say: **"Read signs"**
- Aurie reads text and detects arrows

### Flashlight Control
- Say: **"Turn flashlight on"** or **"Turn flashlight off"**
- Or let auto-mode handle it based on lighting

---

## 🏗️ Architecture

### Core Components

```
com.example.aroundme/
├── assistant/
│   └── AurieAssistant.kt          # Voice assistant singleton
├── camera/
│   ├── CameraManager.kt           # CameraX management
│   └── FlashlightController.kt    # Flashlight control
├── detection/
│   ├── YoloObjectDetector.kt      # YOLOv5 TFLite detector
│   ├── ObjectDetectionAnalyzer.kt # Real-time frame analyzer
│   └── ImageUtils.kt              # Image preprocessing
├── ocr/
│   └── OcrService.kt              # ML Kit OCR
├── ui/
│   └── theme/                     # Jetpack Compose theme
└── MainActivity.kt                # Main entry point
```

### Technology Stack
- **UI:** Jetpack Compose + Material 3
- **Camera:** CameraX
- **ML:** TensorFlow Lite 2.14.0
- **OCR:** ML Kit Text Recognition
- **Speech:** Android SpeechRecognizer + TextToSpeech
- **Async:** Kotlin Coroutines

---

## 🎯 Distance Detection Logic

```kotlin
DANGER:   > 35% of screen area  → "very close - slow down" + double vibration
WARNING:  15-35% of screen     → "approaching" + single vibration
SAFE:     5-15% of screen      → "at safe distance"
FAR:      < 5% of screen       → "far away"
```

### Position Detection
- **Left:** Object in left third of frame → "on your left"
- **Center:** Object in middle third → "in front of you"
- **Right:** Object in right third → "on your right"

---

## 🧪 Testing Checklist

### Basic Functionality
- [ ] App launches without crashes
- [ ] Camera preview displays
- [ ] Aurie speaks welcome message
- [ ] Object detection works in real-time

### Voice Commands
- [ ] "Switch to continuous mode" → Mode changes
- [ ] "Switch to normal mode" → Mode changes back
- [ ] "What's around me?" → Describes environment
- [ ] "Read signs" → OCR activates
- [ ] "Turn flashlight on" → Light turns on
- [ ] "Turn flashlight off" → Light turns off

### Detection & Alerts
- [ ] Vibration occurs before speech
- [ ] Danger objects trigger double vibration
- [ ] Warning objects trigger single vibration
- [ ] Position (left/center/right) is correct
- [ ] Distance levels are accurate

### Lighting
- [ ] Flashlight auto-enables in dark room
- [ ] Flashlight auto-disables in bright room
- [ ] Manual control overrides auto mode

### Edge Cases
- [ ] App handles noisy environments
- [ ] Speech recognition auto-restarts after errors
- [ ] No duplicate announcements for same object
- [ ] Smooth mode switching
- [ ] OCR handles no-text scenarios gracefully

---

## 🔧 Troubleshooting

### Model Not Found Error
**Problem:** `yolov5n.tflite` not found in assets

**Solution:**
```bash
cd tools/python
python download_yolo_model.py
```

### Speech Recognition Not Working
**Problem:** Aurie doesn't respond to voice commands

**Solutions:**
1. Check microphone permission granted
2. Ensure device isn't muted
3. Try saying commands clearly
4. Check Logcat for speech recognition errors

### Camera Permission Denied
**Problem:** Black screen, no camera preview

**Solution:**
1. Go to: Settings > Apps > ARound Me > Permissions
2. Enable Camera and Microphone
3. Restart the app

### Build Errors
**Problem:** Gradle sync fails or build errors

**Solutions:**
1. File > Invalidate Caches > Invalidate and Restart
2. Check internet connection (for dependencies)
3. Update Android Studio to latest version
4. Check `build.gradle.kts` for dependency conflicts

### Vibration Not Working
**Problem:** No haptic feedback

**Solution:**
1. Check device vibration settings
2. Ensure Vibrate permission is in manifest (it is)
3. Test on different device (some emulators don't support vibration)

---

## 📊 Performance Metrics

- **Inference Time:** 50-150ms per frame (YOLOv5n)
- **Frame Processing:** Async (non-blocking)
- **Speech Latency:** < 500ms after command
- **Vibration Response:** Immediate (< 50ms)
- **Battery Impact:** Moderate (camera + ML + continuous speech recognition)

---

## 🚀 Future Enhancements

- [ ] Depth estimation for accurate distance
- [ ] Indoor/outdoor navigation guidance
- [ ] Facial recognition for familiar people
- [ ] Obstacle memory and route learning
- [ ] Multi-language support
- [ ] Cloud-based advanced OCR
- [ ] Wearable integration (smartwatch alerts)
- [ ] Sound source detection
- [ ] Currency/barcode recognition

---

## 📄 License

This project is built for accessibility and assistive technology purposes.

---

## 🙏 Acknowledgments

- **YOLOv5** by Ultralytics
- **TensorFlow Lite** by Google
- **ML Kit** by Google
- **CameraX** by Android Jetpack
- **Jetpack Compose** by Android

---

## 📞 Support

For issues or questions:
1. Check logs: `adb logcat | grep -E "Aurie|ObjectDetection|YoloDetector"`
2. Review this README carefully
3. Test on physical device (not emulator)

---

**Built with ❤️ for accessibility**

*Aurie: Your trustworthy companion for safe navigation*
