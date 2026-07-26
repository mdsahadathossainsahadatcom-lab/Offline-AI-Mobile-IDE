# 📱 Offline Local AI Mobile IDE

> Powered by **llama.cpp**, **Kotlin**, and **Jetpack Compose** — Complete offline mobile development environment with autonomous ReAct agent workflow and local GGUF AI inference.

---

## 🌟 Overview

**Offline Local AI Mobile IDE** is a full-featured, privacy-focused mobile integrated development environment designed to run completely on-device without internet dependencies. Build, edit, preview, and persist mobile web and script projects using high-performance local AI models powered by GGUF quantizations.

---

## ✨ Core Features

- 🤖 **Autonomous ReAct Agent Workflow**: Multi-step AI agent capable of reading workspace files, planning code edits, executing tool calls, and verifying builds autonomously on-device.
- ⚡ **On-Device GGUF Inference**: Native C++ execution via `llama.cpp` supporting models like **Gemma 2B**, **Qwen 2.5 1.5B/3B**, **Phi-3 Mini**, and **Llama 3.2 1B**.
- 📂 **Multi-File Workspace Management**: Full project drawer supporting creation, deletion, live tab editing, and direct ZIP archive import/export.
- 📱 **Interactive Live Preview**: Instant HTML/CSS/JavaScript and web view rendering with live console output and DOM preview.
- 💾 **Room DB History & Persistence**: Full history tracking for project files, chat history sessions, model configurations, and agent execution steps with local JSON export/backup capabilities.
- 🎨 **Material Design 3 Modern UI**: Fluid edge-to-edge layout, responsive keyboard insets handling (`WindowInsets.ime`), custom dark/light themes, and interactive syntax highlighting.

---

## 📐 Architecture & System Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Jetpack Compose UI                               │
│     [Workspace]     [Code Editor]     [Live Preview]     [Local AI Agent]  │
└──────────────────────┬──────────────────────────────────────┬───────────────┘
                       │                                      │
                       ▼                                      ▼
┌──────────────────────────────────────┐    ┌─────────────────────────────────┐
│             IdeViewModel             │    │    Autonomous ReAct Engine     │
│   • Workspace Repository             │◄───┤   • Step Planning & Loop       │
│   • Room Database (SQLite)           │    │   • Tool Execution Engine       │
└──────────────────────────────────────┘    └────────────────┬────────────────┘
                       │                                     │
                       ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Native C++ llama.cpp JNI                            │
│           • Local GGUF Quantized Inference (Q4_K_M / Q8_0)                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📸 Screen Previews

| Workspace & Files | Code Editor & Syntax | Local AI Agent & ReAct |
| :---: | :---: | :---: |
| ![Workspace Preview](https://raw.githubusercontent.com/placeholder/workspace.png) | ![Editor Preview](https://raw.githubusercontent.com/placeholder/editor.png) | ![AI Agent Preview](https://raw.githubusercontent.com/placeholder/agent.png) |

---

## 🛠️ How to Build & Run Locally

### Prerequisites

- **Android Studio**: Jellyfish (2023.3.1+) or newer
- **JDK**: Version 17
- **Android SDK**: API Level 34 (Android 14)
- **Min SDK**: API Level 26 (Android 8.0)

### Step-by-Step Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/offline-local-ai-mobile-ide.git
   cd offline-local-ai-mobile-ide
   ```

2. **Grant Execution Permissions for Gradle Wrapper**:
   ```bash
   chmod +x gradlew
   ```

3. **Compile Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Connected Android Device or Emulator**:
   ```bash
   ./gradlew installDebug
   ```

5. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🚀 Automated CI/CD (GitHub Actions)

This repository includes a continuous integration workflow configured in `.github/workflows/android.yml`. On every `push` or `pull_request` to `main` or `master`, GitHub Actions automatically:
1. Sets up JDK 17 environment.
2. Compiles the debug APK (`./gradlew assembleDebug`).
3. Runs unit tests (`./gradlew testDebugUnitTest`).
4. Uploads the generated APK as a downloadable release artifact named `Offline-AI-IDE-v1.0.apk`.

---

## 📄 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.
