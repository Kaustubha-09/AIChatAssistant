# AI Chat Assistant — Portfolio Case Study

Skim time: 3 minutes.

## The brief

Build a focused, dependency-light Android chat client for OpenAI-compatible LLM endpoints — Material 3, streaming responses, persistent local history, runtime model swap, and an offline mock provider so the UI is driveable without an API key. No Hilt, no Compose, just clean-architecture Java.

## The build

31 Java source files. Single Activity, single Fragment, three architectural layers (UI / Domain / Data), manual DI via a `ServiceLocator` singleton. Room for persistence, Retrofit + OkHttp for HTTP (OkHttp directly for the streaming path), `Resource<T>` wrapper for async state.

## The engineering I'd defend

### 1. Mock provider streams the same pipeline as the real one

`MockStreamingDataSource` doesn't return a canned `"Hello!"` atomically. It emits tokens through the same `Resource.Loading → Resource.Success` pipeline as `RemoteChatDataSource`. The typing-indicator animation, partial-render logic, and final-collapse all exercise on the mock path. Toggling Mock Provider in Settings drives every UX state visible during a real chat — without an API call. See [decisions.md, ADR-003](decisions.md#adr-003--mock-provider-streams-the-same-pipeline-as-the-real-one).

### 2. OkHttp directly for streaming, Retrofit only for non-streaming

Retrofit's `Call.execute()` buffers the full response before returning. That defeats `stream: true` — we'd see the assistant's reply only after generation finished. The streaming path uses OkHttp directly with SSE chunk parsing line-by-line. See [ADR-004](decisions.md#adr-004--okhttp-direct-for-streaming-retrofit-only-for-non-streaming).

### 3. `Resource<T>` wrapper over three booleans

```java
LiveData<Resource<List<Message>>> messages;
```

One stream, three exhaustive states (`Loading`, `Success`, `Error`). The classic three-boolean pattern (`isLoading`, `error`, `data`) admits invalid combinations; the wrapper makes them unrepresentable. Same intuition as `ViewState<T>` in the iOS sibling project [voya](https://github.com/Kaustubha-09/voya). See [ADR-005](decisions.md#adr-005--resourcet-over-three-livedata-streams).

### 4. Manual DI via `ServiceLocator`, not Hilt

The dependency graph is ~10 objects. Hilt's annotation processor adds build time, generated code, and `@HiltAndroidApp` / `@AndroidEntryPoint` ceremony. At this scale, manual DI reads top-to-bottom in a single file and is faster to debug. The cost is testability via `@VisibleForTesting setForTest()` — acceptable. See [ADR-001](decisions.md#adr-001--manual-servicelocator-instead-of-hilt--dagger).

### 5. API key never in source

`app/build.gradle` reads `OPENAI_API_KEY` from `~/.gradle/gradle.properties` or the environment. Empty fallback so the app builds without a key (mock provider is the default). `.gitignore` excludes `local.properties` and `*.keys`. See [ADR-006](decisions.md#adr-006--api-key-loaded-from-gradleproperties-never-hardcoded).

## The honest part

- **No multi-chat support.** Single conversation. Roadmap Phase 1 adds a `chats` table + Jetpack Navigation drawer.
- **No markdown rendering.** Assistant code blocks come out as plain text. Roadmap Phase 2 adds Markwon.
- **No encryption at rest.** The user's API key (when pasted at runtime) lives in plain `SharedPreferences`. Roadmap Phase 3 migrates to `EncryptedSharedPreferences`.
- **Sparse unit tests.** Infrastructure is there (JUnit + Espresso); real coverage of `PromptBuilder`, `Resource<T>` mapping, and `MockStreamingDataSource` is a follow-up.
- **No request cancellation on Fragment destruction.** An in-flight SSE stream keeps consuming tokens after the user navigates away. Small follow-up tracked in [limitations.md](limitations.md).

These aren't hidden TODOs — they're listed in [limitations.md](limitations.md) and the relevant roadmap phases.

## What I'd do next

Roadmap Phase 1: add the conversation list. That's the single biggest UX gap and forces the architectural move from single-Fragment to Jetpack Navigation, which unlocks Phases 3–5.

## What this signals to a recruiter

- I can architect a non-trivial Android app without resorting to enterprise frameworks (Hilt, Compose).
- I understand the difference between *demonstrating streaming* and *streaming through every UX state* — and I built a mock that does the latter.
- I know when Retrofit is the right tool (request building + non-streaming) and when to drop to OkHttp (true SSE streaming).
- I caught a real API key in `build.gradle` before publishing, fixed the loading path, and documented the policy in `ADR-006`. Defensive engineering on credentials is a basic skill that lots of resumes skip.
- I write code I can delete: the `Resource<T>` wrapper, the `ServiceLocator` manual DI, the single-Fragment shape. Easy to swap in Hilt or Compose later without rewriting business logic.
