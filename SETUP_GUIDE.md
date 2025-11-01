# Quick Setup Guide

## Prerequisites
- Android Studio (latest version)
- Android device with camera (Android 8.0+ / API 26+)
- USB cable for device connection

## Build & Install Steps

### 1. Open Project
```
1. Open Android Studio
2. File → Open
3. Navigate to YOLOv8-TfLite-Object-Detector folder
4. Click OK
```

### 2. Sync Gradle
```
1. Android Studio will automatically start syncing
2. Wait for "Gradle sync finished" message
3. If errors occur, click "Sync Project with Gradle Files"
```

### 3. Connect Device
```
1. Enable USB Debugging on your Android device:
   - Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back → Developer Options
   - Enable "USB Debugging"

2. Connect device via USB
3. Accept USB debugging authorization on device
```

### 4. Build & Run
```
1. Select your device in Android Studio toolbar
2. Click Run button (green play icon) or press Shift+F10
3. App will build and install automatically
```

## First Launch Setup

### Grant Permissions
When app launches for the first time:

1. **Camera Permission** (Required)
   - Tap "Allow" when prompted
   - Needed for object detection

2. **Audio Permission** (Optional)
   - Tap "Allow" for voice commands
   - Can skip if not using voice features

3. **Note**: Vibrate permission is auto-granted

### Initial Configuration

1. **Wait for "Navigation system ready"** voice message
2. **GPU Toggle**: ON by default (keep it ON for best performance)
3. **Navigation Toggle**: ON by default (auto-announces obstacles)
4. **Voice Commands**: OFF by default (enable if needed)

## Quick Feature Test

### Test Camera & Detection
1. Point camera at objects (chair, table, person, etc.)
2. You should see colored bounding boxes
3. Check inference time in top-right corner

### Test Navigation
1. Ensure "Navigation" toggle is ON
2. Point camera at nearby object
3. Listen for voice guidance

### Test Voice Commands
1. Enable "Voice Cmd" toggle
2. Grant microphone permission if prompted
3. Say "What do you see?"
4. Listen for response

### Test Manual Controls
1. Tap "Scan" button
2. Listen for object count
3. Tap "Describe" button
4. Listen for detailed description

## Troubleshooting Build Issues

### Gradle Sync Failed
```
1. File → Invalidate Caches / Restart
2. Build → Clean Project
3. Build → Rebuild Project
```

### SDK Issues
```
1. Tools → SDK Manager
2. Install Android 13.0 (API 33) or higher
3. Install Build Tools 34.0.0
```

### Dependency Issues
```
1. Check internet connection
2. File → Sync Project with Gradle Files
3. Wait for downloads to complete
```

### Device Not Detected
```
1. Check USB cable connection
2. Try different USB port
3. Restart ADB:
   - Tools → Troubleshoot Device Connections
   - Click "Restart ADB Server"
```

## Testing Checklist

- [ ] App installs successfully
- [ ] Camera permission granted
- [ ] Camera preview shows
- [ ] Objects are detected with boxes
- [ ] Inference time displays
- [ ] TTS announces "Navigation system ready"
- [ ] TTS announces detected obstacles
- [ ] Scan button works
- [ ] Describe button works
- [ ] Voice commands work (if enabled)
- [ ] Phone vibrates for close objects
- [ ] All toggles respond properly

## Performance Tips

### For Best Frame Rate
- Enable GPU acceleration (ON by default)
- Close background apps
- Use modern device (2020+)

### For Battery Life
- Lower screen brightness
- Disable GPU if phone gets hot
- Close app when not in use

### For Best Audio
- Use headphones
- Set volume to comfortable level
- Test in quiet environment first

## Common Issues & Solutions

### No Voice Output
- Check volume is not muted
- Check TTS is installed (Settings → Accessibility → Text-to-speech)
- Toggle Navigation OFF and ON again

### Voice Commands Not Working
- Grant microphone permission
- Reduce background noise
- Speak clearly at normal pace
- Check Voice Cmd toggle is ON

### Slow Performance
- Enable GPU acceleration
- Close background apps
- Restart app
- Restart device

### App Crashes
- Check Android version (8.0+ required)
- Clear app data (Settings → Apps → ARound Navigator → Clear Data)
- Reinstall app

## Development Notes

### Model Files
The app includes:
- `model.tflite` - YOLOv8 model file
- `labels.txt` - Object class labels

Located in: `app/src/main/assets/`

### Customization
To modify:
- **Announcement frequency**: Edit `announcementInterval` in NavigationManager.kt (default: 2000ms)
- **Detection confidence**: Edit `CONFIDENCE_THRESHOLD` in Detector.kt (default: 0.3)
- **Colors**: Edit `colors.xml`
- **UI layout**: Edit `activity_main.xml`

### Adding New Features
1. Core detection: Modify `Detector.kt`
2. Navigation logic: Modify `NavigationManager.kt`
3. Voice commands: Modify `VoiceCommandHandler.kt`
4. UI changes: Modify `activity_main.xml` and `MainActivity.kt`

## Need Help?

### Documentation
- `README.md` - Full feature documentation
- `ACCESSIBILITY_GUIDE.md` - User guide for accessibility
- `IMPLEMENTATION_SUMMARY.md` - Technical details

### Logs
View Android Studio Logcat for error messages:
```
View → Tool Windows → Logcat
Filter: "ARound" or "Navigation"
```

### Support
For issues or questions:
1. Check documentation first
2. View Logcat for errors
3. Create GitHub issue with:
   - Android version
   - Device model
   - Error message
   - Steps to reproduce

---

**Ready to Build!** 🚀

Open the project in Android Studio and click Run to get started.
