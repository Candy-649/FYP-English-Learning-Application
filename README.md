# EverydayEnglish

**EverydayEnglish** is an Android application for practicing English grammar through adaptive, AI-assisted sentence exercises. It was built as a Final Year Project (FYP) at the Faculty of Information Science and Technology, Universiti Kebangsaan Malaysia (UKM), under course TK/TM/TU/TH4086.

The app is distributed via **GitHub Releases** rather than the Google Play Store, with the target audience including users in mainland China — several common dependencies (Firebase, Hugging Face, LanguageTool) are unreliable or blocked there, so the app is designed to degrade gracefully offline.

---

## Features

- **Adaptive exercise selection** — a Sliding-Window UCB (SW-UCB) multi-armed bandit continuously targets the grammar tenses/categories a user is weakest in.
- **Three-layer answer evaluation pipeline**:
  1. Offline grammar checking via an on-device ONNX/BERT model (works with no network).
  2. Semantic similarity scoring against reference answers via a Hugging Face sentence-embedding model.
  3. Final correctness judgment and Markdown-formatted feedback from DeepSeek V3.
- **Tree-based navigation** with dual-pane support for tablets/landscape layouts, replacing a traditional flat back-stack.
- **Firebase-backed sync** — anonymous auth on first launch, optional account linking, and Firestore-based last-write-wins sync of user data and progress across devices.
- **Progress tracking** — history, streaks, and statistics (charted with Vico).
- **Offline-first** — core practice loop (exercises, offline grammar check, local history) works without network access; online features enhance rather than gate the experience.
- **In-app update checks** — polls the GitHub Releases API (with a Gitee fallback for mainland China users) to notify users of new versions.

---

## Screens

| Screen | Purpose |
|---|---|
| **Main** | Dashboard/home screen, entry point into exercises and other features |
| **Exercise** | Sentence-practice flow: prompt, answer input, evaluation, feedback |
| **History** | Past exercise attempts and outcomes |
| **Statistics** | Charts and breakdowns of performance by tense/category |
| **Profile** | User avatar, display name, account info |
| **Settings** | App preferences, daily goal, account management |

All screens support a dual-pane tablet/landscape layout via the tree-based navigation system.

---

## Architecture

The app follows **MVVM** with manual dependency injection (no Hilt/Koin) via `AppContainer` and `AppViewModelProvider`.

```
UI (Jetpack Compose) → ViewModel → Repository → Room / Firebase / Network
```

### Key components

- **`AppContainer` / `AppDataContainer`** — central manual DI graph. Wires up Room DAOs, offline repositories, `Syncing*` decorator repositories, the grammar/semantic/feedback pipeline, and Firebase-backed services.
- **`SwUcbBandit`** (adaptive exercise engine) — a Sliding-Window UCB bandit with 11 `TenseCategory` arms (`windowSize = 30`, `explorationC = 2.0`). Selects the next exercise category by inverting recent accuracy (`1.0 - μ`) plus an exploration bonus, favoring categories the user struggles with while still exploring under-sampled ones.
- **`SmartGrammarChecker`** — routes grammar checking to an online LanguageTool call when available, falling back to an on-device ONNX/BERT model when offline.
- **`HuggingFaceSemanticChecker`** — scores semantic similarity between the user's answer and reference answers via the Hugging Face Inference API (`all-MiniLM-L6-v2`, threshold `0.75`).
- **`DeepSeekFeedbackGenerator`** — makes the final correctness call and generates Markdown feedback using DeepSeek V3, returned as a single structured JSON response.
- **`TreeNavController`** — custom tree-based navigation controller (replacing an earlier flat back-stack design) that models navigation as a binary tree of nodes, enabling clean dual-pane tablet layouts and correct back/prune semantics.
- **`Syncing*Repository`** decorators (e.g. `SyncingUserProfileRepository`, `SyncingRecordRepository`, `SyncingAttemptRepository`) — wrap offline Room-backed repositories and transparently sync changes to Firestore, using last-write-wins conflict resolution on an `updatedAt` timestamp.
- **`FirebaseAvatarStorageRepository`** — uploads/downloads profile images via Firebase Storage, exposing HTTPS download URLs (not local `file://` URIs) for use with Coil.

### Auth flow

1. Firebase **Anonymous Auth** on first launch.
2. User can continue anonymously, or **Log In** / **Sign Up**.
3. On registration, `linkWithCredential()` upgrades the anonymous account in place, preserving the existing UID and all locally/synced data.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Local database | Room |
| Architecture | MVVM, manual DI (`AppContainer`) |
| Async | Kotlin Coroutines / Flow |
| Image loading | Coil |
| Charts | Vico 2.x |
| Markdown rendering | Markwon |
| Networking | OkHttp |
| Cloud auth / sync | Firebase Auth, Firestore, Firebase Storage |
| Offline grammar checking | ONNX Runtime (quantized INT8 BERT model) |
| Online grammar checking | LanguageTool public API |
| Semantic scoring | Hugging Face Inference API (`router.huggingface.co`) |
| AI feedback generation | DeepSeek V3 (`deepseek-chat`, OpenAI-compatible API) |
| Distribution | GitHub Releases, with Gitee fallback |

---

## Project Structure (partial)

```
Application/app/src/main/java/com/example/everydayenglish/
├── MainActivity.kt
├── AppViewModelProvider.kt
├── adaptiveEngine/          # SW-UCB bandit: SwUcbBandit, ArmState, TenseCategory
├── data/
│   ├── AppContainer.kt      # Manual DI graph
│   ├── entity/              # Room entities (UserProfile, ExerciseRecord, etc.)
│   ├── dao/                 # Room DAOs
│   ├── Repository/          # Repository interfaces
│   ├── OfflineRepository/   # Room-backed implementations
│   ├── FirebaseRepository/  # Firebase Auth / Firestore / Storage implementations
│   └── SyncingRepository/   # Decorators syncing offline data to Firebase
├── domain/                  # e.g. CorrectAnswerRewardApplier
├── grammarChecker/          # GrammarChecker interface, ONNX-based checker
├── onlineEvaluation/        # LanguageTool, Hugging Face, DeepSeek integrations
├── navigation/              # TreeNavController, NavNode, ScreenContent
├── ui/                      # Compose screens (Main, Exercise, History, Statistics, Profile, Settings, Auth)
└── viewmodel/                # ExerciseViewModel, ProfileViewModel, HistoryViewModel, etc.
```

---

## Setup

### Prerequisites

- Android Studio (recent stable version)
- JDK as required by the Android Gradle Plugin in use
- API keys for:
  - Hugging Face Inference API
  - DeepSeek API
  - A Firebase project (Auth, Firestore, Storage enabled)

### Configuration

API keys are injected via `local.properties` → `BuildConfig`, and are **not** committed to the repository:

```properties
# local.properties
HF_API_TOKEN=your_huggingface_token
DEEPSEEK_API_KEY=your_deepseek_key
```

These are exposed to the app via `buildConfigField` entries in `app/build.gradle.kts`, with `buildFeatures { buildConfig = true }` set at the `android {}` block level.

A Firebase project's `google-services.json` must also be placed under `Application/app/`.

### Building

1. Open the `Application/` folder in Android Studio.
2. Sync Gradle.
3. Add `local.properties` values and `google-services.json` as above.
4. Run on an emulator or device.

### ONNX Model / APK Size Notes

- The bundled ONNX grammar-checking model is a quantized INT8 BERT model (~110 MB uncompressed), stored in `assets/` and copied to `filesDir` on first run.
- `useLegacyPackaging = true` is required to work around a known upstream 16 KB page-size alignment limitation affecting `libonnxruntime4j_jni.so`.
- Because of the model's size, hosting/distribution for mainland Chinese users uses cloud object storage (e.g. Alibaba Cloud OSS / Tencent Cloud COS) rather than GitHub/Gitee attachment limits.

---

## Distribution

The app is distributed via [GitHub Releases](https://github.com/Candy-649/FYP-English-Learning-Application/releases) rather than the Play Store. On launch, the app checks the GitHub Releases API for a newer version and prompts the user to update if one is available, falling back to Gitee for users where GitHub access is unreliable.

---

## Development Notes

- No git version control is used for day-to-day iteration in this workflow; files are managed manually.
- Debugging relies primarily on `Log.d` + `adb logcat`.
- Code comments are written in English throughout the codebase.

---

## License

*(Add license information here.)*
