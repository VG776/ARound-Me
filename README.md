# ARound Navigator - Indoor Navigation for Visually Impaired Users

An enhanced YOLOv8-based Android application that provides real-time object detection with navigation assistance and text-to-speech guidance for blind and visually impaired users.

## 🌟 Features

### Core Features
- **Real-time Object Detection** using YOLOv8 TensorFlow Lite model
- **GPU Acceleration** support for faster inference
- **High Performance** optimized for mobile devices

### Accessibility Features

#### 1. **Text-to-Speech Navigation** 🔊
- Real-time audio guidance about detected obstacles
- Smart announcement system (announces every 2 seconds to avoid spam)
- Distance estimation: Very Close, Close, Medium, Far
- Position awareness: Left, Center, Right
- Automatic navigation suggestions based on obstacle position

#### 2. **Haptic Feedback** 📳
- Vibration patterns indicate danger levels
- Different vibration intensities for:
  - Very dangerous obstacles (very close)
  - Dangerous obstacles (close)
  - Moderate obstacles

#### 3. **Voice Commands** 🎤
Hands-free operation with voice commands:
- **"What do you see?"** - Describes detected objects
- **"Describe scene"** - Detailed scene description with positions
- **"Pause speech"** - Stops navigation announcements
- **"Resume"** - Continues navigation
- **"Scan"** - Manual environment scan
- **"Help"** - Lists available commands

#### 4. **Manual Controls**
- **Scan Button** - Manually trigger environment scan
- **Describe Button** - Get detailed scene description
- **Navigation Toggle** - Enable/disable audio guidance
- **Voice Command Toggle** - Enable/disable voice control

### Enhanced UI
- **Clean, Modern Design** with Material Design 3
- **High Contrast** colors for better visibility
- **Large Touch Targets** for easier interaction
- **Gradient overlays** for better readability
- **Color-coded bounding boxes**:
  - 🔴 Red (dashed): Very close objects - DANGER
  - 🟡 Yellow: Close objects - WARNING
  - 🟢 Green: Normal distance objects
- **Confidence scores** displayed with each detection
- **Detection count** shown in real-time

## 📱 UI Components

### Top Info Bar
- App title
- Inference time (processing speed)
- Number of detected objects

### Bottom Control Panel
- Navigation toggle switch
- Voice commands toggle switch
- Scan button
- Describe button
- GPU acceleration toggle

## 🚀 How It Works

### Object Detection
1. Camera captures video frames in real-time
2. YOLOv8 TFLite model processes each frame
3. Objects are detected with bounding boxes
4. Confidence scores are calculated

### Navigation System
1. Detected objects are analyzed for:
   - Position (left, center, right)
   - Distance (based on object height)
   - Danger level (combination of distance and size)
2. Navigation messages are generated
3. Text-to-speech announces guidance
4. Haptic feedback provides additional warning

### Example Navigation Messages
- "Warning! Very close chair in front. Stop or move aside"
- "Close table on your left. Move right"
- "Person ahead on your right"

## 🔧 Technical Details

### Permissions Required
- **CAMERA** - For object detection
- **RECORD_AUDIO** - For voice commands
- **VIBRATE** - For haptic feedback

### Dependencies
- AndroidX Core, AppCompat, Material Design
- CameraX for camera operations
- TensorFlow Lite for inference
- TensorFlow Lite GPU for acceleration

### Performance
- Real-time detection (30+ FPS on modern devices)
- GPU acceleration for 2-3x faster inference
- Optimized TTS announcements to avoid audio spam
- Efficient battery usage

## 🎯 Use Cases

1. **Indoor Navigation**
   - Navigate through rooms and hallways
   - Avoid furniture and obstacles
   - Find doorways and openings

2. **Object Recognition**
   - Identify objects in the environment
   - Get position and distance information
   - Receive navigation suggestions

3. **Scene Understanding**
   - Get overall scene description
   - Understand spatial layout
   - Make informed navigation decisions

## 📖 Usage Instructions

### Getting Started
1. Launch the app
2. Grant camera permission when prompted
3. Point camera at your surroundings
4. Listen to navigation guidance

### Using Voice Commands
1. Enable "Voice Cmd" toggle
2. Grant microphone permission
3. Say commands like "What do you see?"
4. Wait for the response

### Manual Scanning
1. Tap "Scan" button to hear current detections
2. Tap "Describe" for detailed scene information
3. Use these when you need specific information

### Tips for Best Results
- Hold phone steady for accurate detection
- Enable GPU for faster processing
- Use in well-lit environments
- Keep volume at comfortable level
- Enable haptic feedback for additional awareness

## 🔐 Privacy
- All processing happens on-device
- No data is sent to external servers
- Camera feed is not recorded or stored
- Voice commands processed locally

## 🛠️ Customization

You can customize:
- Announcement frequency (default: 2 seconds)
- Confidence threshold (default: 0.3)
- Distance thresholds
- Vibration patterns
- TTS speech rate

## 📄 License
This project builds upon YOLOv8 TensorFlow Lite implementation with additional accessibility features for helping visually impaired users navigate safely.

## 🤝 Contributing
Contributions are welcome! Please feel free to submit issues or pull requests to improve accessibility features.

## 📞 Support
For issues or feature requests, please open an issue on GitHub.

---
**Made with ❤️ for accessibility**
