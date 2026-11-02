# EVA AI — Native Android Intelligent Assistant

[![Android CI/CD](https://github.com/rifatcoder/eva-ai/actions/workflows/android.yml/badge.svg)](https://github.com/rifatcoder/eva-ai/actions/workflows/android.yml)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![Developer](https://img.shields.io/badge/Developer-Aura_RIFAT-8A7FFF)

EVA AI is a next-generation native Android assistant application built using Kotlin, Jetpack Compose, Material 3, and modern Clean MVVM architecture.

---

## 📱 Project Specifications

- **Application Name:** EVA AI
- **Developer:** Aura RIFAT
- **Package Name / Application ID:** `com.evai.rifatcoder`
- **Minimum SDK:** Android 7.0 (API 24)
- **Target / Compile SDK:** Android 16 (API 36, Release 36.1)
- **Language:** Kotlin 2.2.10
- **UI Toolkit:** Jetpack Compose with Material Design 3
- **Build System:** Gradle Kotlin DSL (`.gradle.kts`) with Version Catalog (`libs.versions.toml`)
- **CI/CD:** GitHub Actions (`.github/workflows/android.yml`)

---

## 🎨 Visual Design System (Phase 2)

EVA AI follows a luxury dark aesthetic with crisp neon accents and high readability:

- **Main Background:** `#111019`
- **Secondary Background:** `#171622`
- **Surface Cards:** `#1C1B27`
- **Elevated Surfaces:** `#242231`
- **Borders:** Subtle `#2E2B3E`, Active `#4A4466`
- **Primary Accent:** `#8A7FFF` (Vibrant Purple)
- **Secondary Accent:** `#00E5FF` (Electric Cyan)
- **Primary Text:** `#FFFFFF`
- **Secondary Text:** `#B7B5C7`
- **Muted Text:** `#5E5B73`
- **Semantic Colors:**
  - Success: `#10B981` (Accompanied with text + icon)
  - Warning: `#F59E0B`
  - Error: `#EF4444`
  - Info: `#00E5FF`

### Reusable UI Components
- `EvaScreenBackground`: Full-bleed dark surface with atmospheric radial lighting and system insets handling.
- `EvaTopBar`: Standardized header with screen title, subtitle, and accessible back button.
- `EvaCard`: M3 surface card with 16dp rounded corners, subtle border, and elevation.
- `EvaPrimaryButton`: 48dp purple interactive button with loading, pressed, and disabled states.
- `EvaSecondaryButton`: Outlined card button with cyan accent.
- `EvaIconButton`: 48dp touch target with ripple feedback and accessible content description.
- `EvaSectionHeader`: Consistent typographic section headers.
- `EvaStatusIndicator`: Multi-level status indicator (`Available`, `NotConfigured`, `Disabled`, `Error`, `InProgress`) displaying clear text and icons.
- `EvaEmptyState`: Structured, honest placeholder cards for upcoming phases.
- `EvaLoadingIndicator`: Lightweight dual-arc Canvas loading spinner.
- `EvaAnimatedOrb`: Abstract Compose Canvas AI visual with breathing aura and rotating orbital rings.

---

## 🧭 Navigation Architecture

The application uses Jetpack Navigation Compose with a persistent bottom bar across 5 primary destinations:

1. **Home:** Identity hero, animated AI orb, project overview, live AI connection status card, and quick tool actions.
2. **Chat:** Real AI chat experience with message composer, cancel action, status indicators, and real Gemini/OmniRoute delegation.
3. **AI Connection:** Dedicated configuration screen with Keystore AES-GCM encrypted API key storage, model presets, and live connection test.
4. **Voice:** Real-time audio engine placeholder (microphone inactive until Phase 6).
5. **Memory:** Long-term context & privacy placeholder (storage inactive until Phase 7).
6. **Settings:** Structured configuration overview with clickable AI Provider card routing to AI Connection.

---

## 🛠️ How to Build and Run

### Prerequisites
- JDK 17 or higher
- Android SDK with platform tools installed (API 36)

### Build with Gradle Wrapper
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Run Unit and Robolectric Tests
./gradlew testDebugUnitTest
```

The output APKs are located at:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

### 🚀 Automated GitHub Releases
Every push to `main` / `master` triggers the **Android CI/CD & GitHub Release** workflow (`.github/workflows/android.yml`). It automatically runs all unit tests, compiles both **Debug** and **Release** APKs, computes SHA-256 cryptographic hashes, and publishes them as downloadable assets directly onto the repository's **GitHub Releases** page with source code `.zip` and `.tar.gz` archives.

---

## 🗺️ Development Roadmap
- [x] **Phase 1:** Project foundation from scratch (application ID `com.evai.rifatcoder`, Gradle configuration, CI/CD, Git).
- [x] **Phase 2:** Premium design system, centralized tokens, reusable components, persistent bottom navigation, and structural placeholder screens.
- [x] **Phase 3:** Premium Home Dashboard (Sections A–I, reactive MVVM state management, truthful status indicators, and architectural contracts for Accessibility Automation & Four-Side Edge Glow).
- [x] **Phase 4:** AI Chat Screen, message composer, and chat engine foundation.
- [x] **Phase 5:** Real Google Gemini API + OmniRoute API integration with native Keystore AES-GCM encryption, live connection testing, and cancellation.
- [ ] **Phase 6:** Voice Assistant & Offline Wake-Word Detection.
- [ ] **Phase 7:** Long-term Memory Bank & Local Encrypted Persistence.
- [ ] **Phase 8:** Device Tools & Native Hardware Controls.
- [ ] **Phase 9:** Verified Android Accessibility Service Automation.
- [ ] **Phase 10:** Four-Side Screen-Edge & Corner Glow Animation Overlay.
