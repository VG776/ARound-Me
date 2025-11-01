# 🚀 Aurie - Quick Start Guide

## ⚡ 3-Step Setup

### Step 1: Download Model (5 minutes)
```bash
cd tools\python
python download_yolo_model.py
```
**What it does**: Downloads YOLOv5n TFLite model (~8-10 MB) to `app/src/main/assets/`

---

### Step 2: Build & Install (2 minutes)
1. Open project in Android Studio
2. Connect physical Android device (USB debugging ON)
3. Click **Run** ▶️ button
4. Grant **Camera** and **Microphone** permissions

---

### Step 3: Test (1 minute)
1. Wait for: *"Hello, I'm Aurie. I'm here to guide you safely."*
2. Say: **"Hello Aurie"**
3. Expected: *"Hello! How can I help you?"*

✅ **You're all set!**

---

## 🎤 Essential Commands

```
"Switch to continuous mode"  → Continuous environment description
"Switch to normal mode"      → Alert-only mode (default)
"What's around me?"          → Describe surroundings now
"Read signs"                 → Read text via OCR
"Turn flashlight on/off"     → Control camera flash
```

---

## 📳 What to Expect

### Normal Mode (Default)
- 🔇 Quiet unless danger/warning
- 📳 Vibrates when object is close
- 🗣️ Announces: "Chair approaching, on your left"
- 🔋 Battery efficient

### Continuous Mode
- 🔊 Constantly describes environment  
- 📍 "Chair in front, table on right, person far away"
- 🎯 Top 3 objects every few seconds
- 🔋 Higher battery usage

---

## 🎯 Distance System

| Alert | Distance | Vibration | Voice |
|-------|----------|-----------|-------|
| 🔴 DANGER | Very close | Double buzz | "Very close - slow down!" |
| 🟡 WARNING | Approaching | Single buzz | "Approaching" |
| 🟢 SAFE | Medium | None | (Continuous mode only) |
| ⚪ FAR | Far away | None | (Continuous mode only) |

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| ❌ "Model not found" | Run `download_yolo_model.py` script |
| ❌ Black screen | Grant camera permission in Settings |
| ❌ No voice response | Grant microphone permission, check volume |
| ❌ Build error | File > Invalidate Caches > Restart |

---

## 💡 Pro Tips

1. **Point camera forward** while walking
2. **Use normal mode** for daily routines (saves battery)
3. **Use continuous mode** when exploring new places
4. **Speak clearly** at normal volume for commands
5. **In dark rooms**, flashlight auto-enables
6. **For reading signs**, hold camera steady and say "Read signs"

---

## 📊 What Aurie Can Detect

**80 objects including:**
- 👤 People
- 🚗 Vehicles (car, bus, truck, bicycle, motorcycle)
- 🪑 Furniture (chair, table, couch, bed)
- 🐕 Animals (dog, cat, bird, horse)
- 💻 Electronics (laptop, phone, TV, keyboard)
- 🍴 Kitchen items (cup, bottle, bowl, knife)
- And much more!

---

## ⚙️ System Requirements

- ✅ **Android 5.0+** (API 21+)
- ✅ **Physical device** (camera required)
- ✅ **2 GB RAM** minimum
- ✅ **50 MB storage** for app + model
- ✅ **Camera permission** (required)
- ✅ **Microphone permission** (required)

---

## 🎓 First-Time User Flow

```
1. Launch app
   ↓
2. Grant camera permission → ✅
   ↓
3. Grant microphone permission → ✅
   ↓
4. Hear: "Hello, I'm Aurie..."
   ↓
5. Camera preview appears
   ↓
6. Object detection starts automatically
   ↓
7. Say "Hello Aurie" to test
   ↓
8. Walk around, point camera forward
   ↓
9. Feel vibration when near objects
   ↓
10. Hear Aurie announce objects
    ↓
✅ You're navigating safely with Aurie!
```

---

## 📱 UI Elements

**Top Left Corner:**
```
┌─────────────────┐
│ Aurie Active    │ ← Status
│ Normal Mode     │ ← Current mode
└─────────────────┘

┌─────────────────────────────────┐
│ Chair approaching, on your left │ ← Last detection
└─────────────────────────────────┘
```

**Bottom Center:**
```
Say 'Hey Aurie' for commands
```

---

## 🔐 Permissions Explained

| Permission | Why Needed | When Asked |
|------------|------------|------------|
| 📷 Camera | Object detection, OCR | First launch |
| 🎤 Microphone | Voice commands | First launch |
| 📳 Vibrate | Haptic alerts | Auto-granted |
| 💡 Flashlight | Low-light assist | Auto-granted |

---

## 📞 Quick Help

**Build Issues?**
```bash
# Clear cache and rebuild
File > Invalidate Caches > Invalidate and Restart
Build > Clean Project
Build > Rebuild Project
```

**Runtime Issues?**
```bash
# Check logs
adb logcat | grep -E "Aurie|ObjectDetection|YoloDetector"
```

**Model Issues?**
```bash
# Verify model exists
dir app\src\main\assets\yolov5n.tflite
# Should show ~8-10 MB file
```

---

## 📚 More Help

- 📖 **README.md** - Full documentation
- 📋 **QUICK_REFERENCE.md** - Command list
- ✅ **SETUP_CHECKLIST.md** - Detailed testing
- 📊 **IMPLEMENTATION_SUMMARY.md** - Technical details

---

## 🎯 Success Indicators

✅ App launches without crash  
✅ Camera preview visible  
✅ Aurie speaks welcome message  
✅ Voice commands work  
✅ Objects detected in real-time  
✅ Vibration alerts function  
✅ Distance warnings accurate  

**All green?** → You're ready to navigate safely with Aurie! 🎉

---

## ⚡ TL;DR

```bash
# 1. Download model
python tools/python/download_yolo_model.py

# 2. Build & run in Android Studio

# 3. Grant permissions

# 4. Say "Hello Aurie" to test

# 5. Walk around safely!
```

---

**🎉 Welcome to Aurie - Your AI companion for safe navigation!**

*Built with ❤️ for accessibility*
