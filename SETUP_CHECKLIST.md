# Aurie Setup Checklist

Complete this checklist before running the app for the first time.

## ✅ Pre-Build Setup

### 1. Model Files
- [ ] Run `python tools/python/download_yolo_model.py`
- [ ] Verify `app/src/main/assets/yolov5n.tflite` exists
- [ ] Verify `app/src/main/assets/labels.txt` exists
- [ ] Check model size is ~8-10 MB

### 2. Android Studio
- [ ] Latest Android Studio installed
- [ ] Project opened successfully
- [ ] Gradle sync completed without errors
- [ ] No red underlines in Kotlin files

### 3. Device Preparation
- [ ] Physical Android device connected (NOT emulator)
- [ ] USB debugging enabled on device
- [ ] Device recognized by `adb devices` command
- [ ] Device running Android 5.0+ (API 21+)

## 🔨 Build Process

### 4. Dependencies
- [ ] All Gradle dependencies downloaded
- [ ] TensorFlow Lite dependencies resolved
- [ ] ML Kit dependencies resolved
- [ ] CameraX dependencies resolved
- [ ] No build warnings (minor warnings OK)

### 5. Compilation
- [ ] Build > Make Project succeeds
- [ ] No compilation errors in Kotlin code
- [ ] APK generated successfully
- [ ] Ready to run

## 📱 Installation & First Run

### 6. App Installation
- [ ] Run > Run 'app' from Android Studio
- [ ] App installs on device without errors
- [ ] App icon appears on device home screen
- [ ] No installation error dialogs

### 7. Permissions
- [ ] Camera permission dialog appears
- [ ] Camera permission granted
- [ ] Microphone permission dialog appears
- [ ] Microphone permission granted

### 8. Initial Launch
- [ ] App opens without crash
- [ ] Camera preview displays
- [ ] Aurie speaks: "Hello, I'm Aurie..."
- [ ] No error messages in Logcat

## 🧪 Basic Functionality Tests

### 9. Object Detection
- [ ] Point camera at an object
- [ ] Object is detected (check Logcat for "🔍 Detected")
- [ ] Aurie announces object when close
- [ ] Bounding box appears in mental model (check logs)

### 10. Voice Commands
- [ ] Say "Hello Aurie" → Response: "Hello! How can I help you?"
- [ ] Say "Switch to continuous mode" → Confirms mode switch
- [ ] Say "Switch to normal mode" → Returns to normal
- [ ] Speech recognition auto-restarts after command

### 11. Vibration
- [ ] Walk close to an object
- [ ] Feel vibration before Aurie speaks
- [ ] Danger objects: double buzz
- [ ] Warning objects: single buzz

### 12. Flashlight
- [ ] Cover camera lens (simulate darkness)
- [ ] Flashlight auto-enables
- [ ] Uncover lens
- [ ] Flashlight auto-disables
- [ ] Say "Turn flashlight on" → Manual control works

### 13. OCR
- [ ] Point camera at text (sign, label, book)
- [ ] Say "Read signs"
- [ ] Aurie reads the text aloud
- [ ] Arrow detection works (if present)

## 🎯 Advanced Tests

### 14. Mode Switching
- [ ] Switch to continuous mode
- [ ] Aurie describes environment continuously
- [ ] Multiple objects announced
- [ ] Switch back to normal mode
- [ ] Only urgent objects announced

### 15. Distance Detection
- [ ] Walk towards an object
- [ ] Announces "far away" → "at safe distance" → "approaching" → "very close"
- [ ] Vibration intensity increases as distance decreases
- [ ] Alert tone changes for danger zone

### 16. Position Detection
- [ ] Hold object to the left → "on your left"
- [ ] Hold object in center → "in front of you"
- [ ] Hold object to the right → "on your right"
- [ ] Position descriptions are accurate

## 🔍 Logcat Verification

### 17. Check Logs
Open Android Studio Logcat and filter for:
- [ ] `AurieAssistant` - Voice command logs
- [ ] `ObjectDetection` - Detection results
- [ ] `YoloDetector` - Model inference logs
- [ ] `FlashlightController` - Lighting control
- [ ] `OcrService` - OCR results

Expected log patterns:
```
✅ Aurie initialized successfully
✅ YOLOv5 detector initialized successfully
🎤 Recognized: hello aurie
🔊 Speaking: Hello! How can I help you?
🔍 Detected 3 objects
📦 Chair (85%) in front of you, at safe distance
💡 Flashlight turned ON
📳 Vibration alert: DANGER
```

## 🐛 Troubleshooting Checklist

If any test fails:
- [ ] Check Logcat for error messages
- [ ] Verify all permissions granted
- [ ] Restart the app
- [ ] Rebuild the project (Build > Clean + Rebuild)
- [ ] Check model files exist
- [ ] Test on different physical device
- [ ] Review README.md troubleshooting section

## 📊 Performance Check

### 18. Performance Metrics
- [ ] Frame processing: < 200ms per frame
- [ ] Speech response: < 1 second
- [ ] Vibration: Immediate (< 50ms)
- [ ] No significant frame drops
- [ ] App doesn't lag or freeze
- [ ] Battery drain is acceptable

## 🎉 Final Verification

### 19. Complete User Journey
- [ ] Launch app
- [ ] Grant permissions
- [ ] Hear welcome message
- [ ] Test normal mode
- [ ] Switch to continuous mode
- [ ] Test voice commands
- [ ] Test OCR
- [ ] Test flashlight
- [ ] Walk around with app
- [ ] Everything works smoothly!

## ✨ Success Criteria

✅ **App is ready for use when:**
1. No crashes or errors
2. Object detection works in real-time
3. Aurie responds to voice commands
4. Vibration alerts function correctly
5. OCR reads text accurately
6. Flashlight auto-adjusts properly
7. Mode switching is seamless
8. User experience is smooth and helpful

---

## 📝 Notes Section

Use this space to note any issues or observations:

**Build Date**: _______________

**Device Tested**: _______________

**Android Version**: _______________

**Issues Found**: 

```
(List any problems encountered)
```

**Solutions Applied**:

```
(Document how issues were resolved)
```

**Additional Notes**:

```
(Any other observations or improvements needed)
```

---

**Status**: ⬜ Not Started | 🟡 In Progress | ✅ Complete

Mark your overall status: __________

---

*Once all items are checked, Aurie is ready to help users navigate safely!*
