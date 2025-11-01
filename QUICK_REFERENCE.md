# Aurie Quick Reference Guide

## 🎤 Voice Commands

| Command | Action |
|---------|--------|
| "Switch to continuous mode" | Enable continuous environment description |
| "Switch to normal mode" | Return to alert-only mode |
| "What's around me?" | Describe current surroundings |
| "Read signs" | Activate OCR to read text |
| "Turn flashlight on" | Enable camera flash |
| "Turn flashlight off" | Disable camera flash |
| "Hello Aurie" / "Hey Aurie" | Greeting / test command |
| "Help" | List available commands |

## 📳 Vibration Patterns

- **Double Buzz (200ms + 200ms)**: DANGER - Object very close, slow down!
- **Single Buzz (150ms)**: WARNING - Object approaching, be cautious

## 🎯 Distance Levels

| Level | Screen Area | Description | Alert |
|-------|-------------|-------------|-------|
| DANGER | > 35% | "very close - slow down" | Double vibration + Alert voice |
| WARNING | 15-35% | "approaching" | Single vibration + Normal voice |
| SAFE | 5-15% | "at safe distance" | Normal voice |
| FAR | < 5% | "far away" | Continuous mode only |

## 📍 Position Detection

- **Left**: "on your left"
- **Center**: "in front of you"  
- **Right**: "on your right"

## 🔦 Flashlight Auto Mode

- **Dark (< 50 brightness)**: Auto ON
- **Bright (≥ 50 brightness)**: Auto OFF
- Voice commands override auto mode

## 🎛️ Operating Modes

### Normal Mode (Default)
✅ Announces DANGER and WARNING objects only  
✅ Vibrates before speaking  
✅ Alert tone for danger  
✅ Quiet when safe  

### Continuous Mode
✅ Continuously describes environment  
✅ Lists top 3 nearby objects  
✅ Updates as you move  
✅ Calm, conversational tone  

## 🔊 Speech Behavior

- **Priority Messages**: Immediate (greetings, mode switches, OCR results)
- **Alert Messages**: Faster speech, higher pitch (danger objects)
- **Normal Messages**: Calm pace, normal pitch
- **Throttling**: 3-second cooldown for repeated messages
- **Queue**: Non-priority messages queued to avoid overlap

## 🏗️ First-Time Setup

1. **Download Model**
   ```bash
   cd tools/python
   python download_yolo_model.py
   ```

2. **Build in Android Studio**
   - Open project
   - Sync Gradle
   - Build > Make Project

3. **Run on Device**
   - Connect physical Android device
   - Run > Run 'app'
   - Grant Camera and Microphone permissions

4. **Test**
   - Wait for "Hello, I'm Aurie..."
   - Say "Hello Aurie"
   - Walk around to test detection

## 🐛 Quick Troubleshooting

| Problem | Quick Fix |
|---------|-----------|
| No voice response | Check microphone permission, unmute device |
| Black screen | Grant camera permission, restart app |
| Model not found | Run `download_yolo_model.py` script |
| No vibration | Check device vibration settings |
| Build error | Invalidate caches, restart Android Studio |

## 📱 Minimum Requirements

- **Android Version**: API 21+ (Android 5.0 Lollipop)
- **Hardware**: Camera, microphone, vibration motor
- **Device Type**: Physical device (emulator not recommended)
- **Storage**: ~50 MB for app + model
- **RAM**: 2 GB minimum recommended

## 🔋 Battery Optimization

- Camera usage: Moderate drain
- ML inference: Low-moderate drain  
- Speech recognition: Low drain
- Continuous mode: Higher drain than normal mode

**Tip**: Use normal mode for better battery life

## 🎓 Usage Tips

1. **Point camera forward** for best detection
2. **Speak clearly** for voice commands (normal volume)
3. **Wait for Aurie to finish** speaking before next command
4. **Use continuous mode** when exploring new environments
5. **Use normal mode** for familiar routes
6. **Indoor low light**: Let auto-flashlight help
7. **OCR works best** with well-lit, clear text

## 📊 Detection Classes (COCO)

Aurie can detect 80 object types including:
- People, vehicles (car, bus, truck, bicycle, motorcycle)
- Animals (dog, cat, bird, horse, cow, etc.)
- Furniture (chair, couch, bed, table)
- Electronics (tv, laptop, phone, keyboard)
- Kitchen items (bottle, cup, bowl, knife, fork)
- And many more!

## 🔐 Permissions Required

- ✅ **Camera** (REQUIRED) - Object detection
- ✅ **Microphone** (REQUIRED) - Voice commands
- ✅ **Vibrate** (AUTO) - Haptic alerts
- ✅ **Flashlight** (AUTO) - Low light assistance
- ✅ **Internet** (OPTIONAL) - For future features

---

**Remember**: Aurie is your companion, not a replacement for other mobility aids. Always stay safe!
