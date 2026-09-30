# Jarvis — Personal AI Assistant for Android

Jarvis is a voice-controlled personal AI assistant for Android inspired by Tony Stark's JARVIS. It features voice interaction, multi-turn conversation memory with configurable retention, phone automation protocols, biometric security, and local encrypted vault storage.

## Key Features

1. **Voice Control**
   - Offline Wake Word: Listens for "Hey Jarvis" (or custom phrases like "Jarvis", "Computer") using a battery-aware acoustic detector.
   - Continuous Listening Mode: Maintains an active speech-recognition loop for hands-free conversations.
   - Speech-To-Text (STT): Android SpeechRecognizer with live RMS audio energy meter.
   - Text-To-Speech (TTS): Articulate vocal modulation with customizable pitch and speech rate.
   - Interactive Arc Reactor: Animated rotating concentric rings and audio-reactive energy pulsation.

2. **Conversation Memory System**
   - Configurable Retention Period: 24 Hours, 7 Days, 30 Days, or Forever.
   - Automatic Cache Pruning: Expired turns are purged on startup and preference changes.
   - Context-Aware Multi-Turn Follow-Ups: Feeds recent conversation turns and learned facts into prompt context.
   - Long-Term Knowledge Vault: Automatically extracts and retains user preferences, contacts, and custom facts.

3. **Phone Automation (Intent & Accessibility Engine)**
   - Open Apps: Intelligently resolves and launches installed applications.
   - Phone Calls: Dials or prepares phone calls with confirmation safeguards.
   - SMS & Email: Drafts messages and emails; prompts user with an authorization dialog before dispatching.
   - Alarms & Timers: Sets clock alarms and timers.
   - System Accessibility: `JarvisAccessibilityService` executes global actions (Home, Back, Recents, Notifications, Quick Settings, Lock Screen).
   - Notification Briefing: `JarvisNotificationListenerService` reads and summarizes notifications aloud.

4. **Security & Cryptography**
   - Hardware-Backed Keystore: Chat logs, notes, and memories are encrypted using AES-256-GCM via Android Keystore.
   - Biometric Clearance: `BiometricPrompt` protects access with fingerprint/face scan or PIN fallback.
   - Local-First Architecture: Complete offline fallback intelligence engine when disconnected from network.

## Setup & Configuration

### Gemini API Key
- Jarvis uses `BuildConfig.GEMINI_API_KEY` (configured in `.env` / Secrets panel).
- Alternatively, users can enter a custom API key in the **Settings** tab inside the app.
- If no key is provided or the device is offline, Jarvis falls back to its built-in rule engine.

### Permissions Checklist
- `RECORD_AUDIO`: Voice command recognition and wake-word listener.
- `CALL_PHONE`: Telephony dialing.
- `SEND_SMS`: Drafting and dispatching text messages.
- `READ_CONTACTS`: Resolving contact names to phone numbers.
- `POST_NOTIFICATIONS`: Ongoing wake-word service and status alerts.
- **Accessibility Service**: In Android Settings > Accessibility > Jarvis Assistant.
- **Notification Access**: In Android Settings > Notifications > Device & app notifications > Jarvis.
