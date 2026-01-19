# Smart Parent Lock

An Android application designed to help parents encourage healthy smartphone habits and learning in children through educational challenges.

## 📱 Features

### Core Features
- **Parent Authentication**: Secure PIN-based access control
- **Educational Lock Screen**: Interactive challenges (Math, Vocabulary, Patterns, GK, Translation)
- **Adaptive Difficulty**: Age-based question complexity (5-18 years)
- **Session Management**: Configurable unlock duration (1-60 minutes)
- **Device Admin Protection**: Optional uninstall prevention
- **Emergency Access**: Parent PIN override on lock screen

### Monetization
- **Banner Ads**: Bottom-sticky ad on dashboard (AdMob)
- **Interstitial Ads**: Full-screen ads on lock toggle and app exit

---

## 🏗️ Architecture

### **High-Level Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                     USER INTERFACE LAYER                     │
├─────────────────────────────────────────────────────────────┤
│  PinActivity  │  WelcomeActivity  │  MainActivity  │  Feedback│
│  (Auth)       │  (Onboarding)     │  (Dashboard)   │  Activity│
└────────┬──────────────────┬───────────────┬────────────┬─────┘
         │                  │               │            │
         └──────────────────┴───────────────┴────────────┘
                            │
         ┌──────────────────▼───────────────────────────┐
         │           BUSINESS LOGIC LAYER               │
         ├──────────────────────────────────────────────┤
         │  SettingsRepository  │  ChallengeManager     │
         │  (Data Persistence)  │  (Question Generator) │
         └──────────────────────┴───────────────────────┘
                            │
         ┌──────────────────▼───────────────────────────┐
         │              SERVICE LAYER                   │
         ├──────────────────────────────────────────────┤
         │           LockService (Foreground)           │
         │  • Screen Overlay Management                 │
         │  • Session Timer (Re-lock)                   │
         │  • Challenge Display & Validation            │
         └──────────────────────────────────────────────┘
                            │
         ┌──────────────────▼───────────────────────────┐
         │           SYSTEM INTEGRATION                 │
         ├──────────────────────────────────────────────┤
         │  BootReceiver  │  ScreenReceiver  │  Admin   │
         │  (Auto-start)  │  (Screen events) │  Receiver│
         └──────────────────────────────────────────────┘
```

---

## 📂 Project Structure

```
app/src/main/
├── java/com/smartparentlock/
│   ├── MainActivity.kt              # Dashboard & Settings UI
│   ├── PinActivity.kt               # PIN setup & verification
│   ├── WelcomeActivity.kt           # Onboarding flow
│   ├── FeedbackActivity.kt          # User feedback form
│   ├── LockService.kt               # Core lock overlay service
│   ├── SettingsRepository.kt        # Encrypted data storage
│   ├── ChallengeManager.kt          # Question generation logic
│   ├── BootReceiver.kt              # Auto-start on device boot
│   └── SmartParentAdminReceiver.kt  # Device admin policies
│
├── res/
│   ├── layout/                      # XML layouts
│   ├── drawable/                    # Icons & backgrounds
│   ├── values/                      # Strings, colors, themes
│   └── xml/                         # Device admin policies
│
└── AndroidManifest.xml              # App configuration & permissions
```

---

## 🔧 Component Details

### **1. Activities**

#### **PinActivity**
- **Purpose**: PIN creation and verification
- **Flow**: 
  - First launch → Create 4-digit PIN
  - Subsequent launches → Verify PIN
  - Change PIN → Re-create PIN (skips onboarding)
- **Security**: Encrypted storage via `SettingsRepository`

#### **WelcomeActivity**
- **Purpose**: First-time user onboarding
- **Features**: 
  - ViewPager2 with 4 educational screens
  - Dynamic background colors
  - Page indicators
- **Flow**: Shown once after PIN creation

#### **MainActivity**
- **Purpose**: Parent dashboard for configuration
- **Features**:
  - Lock enable/disable toggle
  - Learning mode configuration
  - Challenge type selection (multi-select)
  - Age-based difficulty slider
  - Session duration control
  - Device admin management
  - AdMob integration (Banner + Interstitial)
- **Session Management**: 5-minute timeout, requires re-authentication

#### **FeedbackActivity**
- **Purpose**: User feedback collection
- **Features**: Email, subject, message fields
- **Integration**: Launches email client with pre-filled data

---

### **2. Services**

#### **LockService** (Foreground Service)
- **Purpose**: Manages lock screen overlay
- **Lifecycle**:
  - Starts on boot (if lock enabled)
  - Runs as foreground service with notification
  - Listens to `ACTION_SCREEN_ON` events
- **Key Functions**:
  - `showOverlay()`: Displays challenge or simple unlock
  - `setupChallenge()`: Generates random question from enabled types
  - `setupPinChallenge()`: Parent authentication mode
  - `startSessionTimer()`: Schedules re-lock after configured duration
  - `handleCorrectAnswer()`: Visual/haptic feedback, removes overlay
  - `handleWrongAnswer()`: Error feedback, regenerates question
- **Session Management**: 
  - `sessionExpiryTime`: Tracks grace period
  - `lockRunnable`: Delayed task to re-lock screen

---

### **3. Data Layer**

#### **SettingsRepository**
- **Purpose**: Encrypted data persistence
- **Storage**: `EncryptedSharedPreferences` (AES256)
- **Data Stored**:
  - Parent PIN (hashed)
  - Lock enabled state
  - Learning mode enabled
  - Challenge types (Set<ChallengeType>)
  - Child age (5-18)
  - Session duration (1-60 minutes)
- **Key Methods**:
  - `getPin()`, `setPin()`, `clearPin()`
  - `isLockEnabled()`, `setLockEnabled()`
  - `getEnabledChallengeTypes()`, `setEnabledChallengeTypes()`

#### **ChallengeManager**
- **Purpose**: Educational question generation
- **Challenge Types**:
  - `MATH`: Arithmetic (age-adaptive)
  - `VOCABULARY`: Word definitions
  - `PATTERNS`: Sequence completion
  - `GK`: General knowledge
  - `TRANSLATION`: Hindi-English
- **Difficulty Scaling**: Questions adapt based on child's age
- **Output**: `Challenge` data class (question, options, correctIndex)

---

### **4. Broadcast Receivers**

#### **BootReceiver**
- **Purpose**: Auto-start `LockService` on device boot
- **Trigger**: `ACTION_BOOT_COMPLETED`
- **Condition**: Only starts if lock is enabled

#### **ScreenReceiver** (Internal to LockService)
- **Purpose**: Detect screen-on events
- **Trigger**: `ACTION_SCREEN_ON`
- **Logic**: Shows overlay only if session has expired

#### **SmartParentAdminReceiver**
- **Purpose**: Device admin capabilities
- **Feature**: Prevents app uninstallation (requires user consent)

---

## 🔐 Security Features

1. **Encrypted Storage**: All sensitive data stored with AES-256 encryption
2. **PIN Protection**: 4-digit PIN required for all parent actions
3. **Session Timeout**: Auto-logout after 5 minutes of inactivity
4. **Emergency Override**: Parent can always unlock via PIN on lock screen
5. **Device Admin**: Optional uninstall protection (removable via PIN)

---

## 💰 Monetization Strategy

### **AdMob Integration**
- **App ID**: Configured in `AndroidManifest.xml`
- **Ad Units**:
  1. **Banner** (320x50): Bottom of dashboard (`activity_main.xml`)
  2. **Interstitial** (Full-screen): 
     - Triggered on lock toggle
     - Triggered on app exit
- **Test Mode**: Uses Google test IDs during development
- **Production**: Replace with real AdMob IDs before release

---

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### **Setup**
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device/emulator (API 24+)

### **First Launch Flow**
1. Create 4-digit Parent PIN
2. Complete onboarding (4 screens)
3. Grant "Display over other apps" permission
4. Configure lock settings
5. Enable lock

---

## 📋 Permissions

| Permission | Purpose | Type |
|------------|---------|------|
| `SYSTEM_ALERT_WINDOW` | Display lock screen overlay | Special |
| `FOREGROUND_SERVICE` | Keep service running | Normal |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Parental control use case | Normal |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot | Normal |
| `BIND_DEVICE_ADMIN` | Uninstall protection | Admin |
| `INTERNET` | AdMob ads | Normal |

---

## 🎯 Play Store Readiness

### **Completed**
- ✅ Target SDK 34 (Android 14)
- ✅ Adaptive icon
- ✅ Privacy Policy link (placeholder)
- ✅ Hardcoded strings extracted to `strings.xml`
- ✅ Light mode enforced (Dark mode disabled)
- ✅ AdMob integrated (Test IDs)

### **Before Publishing**
1. Replace AdMob Test IDs with real IDs
2. Create and host Privacy Policy
3. Update Privacy Policy URL in `MainActivity.kt`
4. Prepare Play Store listing (screenshots, description)
5. Fill out Data Safety section (declare permissions)
6. Provide justifications for special permissions

---

## ⚡ Performance & Safety

- **Battery Efficient**: Event-based triggers (no polling)
- **Low Overhead**: Zero background network usage
- **Optimized Crypto**: Fast PIN verification
- **No Heating**: No intensive rendering or sensor loops
- **Safe for Extended Use**: Minimal resource consumption

---

## 🛠️ Development Notes

### **Key Design Patterns**
- **Repository Pattern**: `SettingsRepository` abstracts data layer
- **Service-Oriented**: `LockService` handles core business logic
- **MVVM-lite**: Activities manage UI, delegate to repositories/managers

### **Testing Recommendations**
1. Test on multiple Android versions (7.0 - 14.0)
2. Verify permission flows on fresh installs
3. Test session timeout behavior
4. Validate challenge difficulty scaling
5. Test AdMob integration (use test device IDs)

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 👨‍💻 Developer

For questions or support, use the in-app feedback feature.

---

**Version**: 1.0  
**Last Updated**: January 2026
