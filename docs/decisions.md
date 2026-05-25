# Architecture Decision Records

Dated decisions, append-only.

## ADR-001 · Manual `ServiceLocator` instead of Hilt / Dagger

**Date:** 2026-03
**Status:** Accepted

A `ServiceLocator` singleton lazily constructs Retrofit, Room DB, and repositories. No Hilt, no Dagger, no Koin.

**Why:** the dependency graph is ~10 objects. Hilt's annotation processor adds build time, generated code to debug, and `@HiltAndroidApp` / `@AndroidEntryPoint` ceremony. For a one-Activity / one-Fragment app, manual DI reads top-to-bottom in a single file. Hilt is worth it at a much larger scale.

**Cost:** the `ServiceLocator` is global state — testability requires injecting a different instance under test. We address this with a `setForTest(...)` method behind a `@VisibleForTesting` annotation.

---

## ADR-002 · Single-Activity / single-Fragment

**Date:** 2026-03
**Status:** Accepted

`MainActivity` hosts exactly one `ChatFragment`. No `NavGraph`, no Jetpack Navigation, no fragment back-stack.

**Why:** the app has one screen plus a bottom sheet. Adding a navigation component for one fragment is over-engineering.

**When to revisit:** when we add a conversation list ([roadmap.md](roadmap.md) — "Conversation list — multiple chats in a left drawer"). That's a multi-screen architecture; we'll pick up Jetpack Navigation then.

---

## ADR-003 · Mock provider streams the same pipeline as the real one

**Date:** 2026-03
**Status:** Accepted

`MockStreamingDataSource` doesn't return a canned `"Hello!"` string atomically. It emits tokens through the same `Resource.Loading → Resource.Success` pipeline that `RemoteChatDataSource` uses, simulating real SSE streaming.

**Why:** the UI's typing-indicator animation, partial-render logic, and final-collapse behavior all need exercise. A mock that returns the full string atomically would skip all that and let UX bugs slip past. Streaming-simulator mock means *every state* the user sees in production is reachable offline.

**Cost:** the mock is a few lines longer. Acceptable.

---

## ADR-004 · OkHttp direct for streaming, Retrofit only for non-streaming

**Date:** 2026-03
**Status:** Accepted

`RemoteChatDataSource.streamChatCompletion(...)` uses raw OkHttp `Call` + response-body stream parsing. The non-streaming path (if any) would use Retrofit.

**Why:** Retrofit's `Call.execute()` buffers the full response body before handing it to the caller. That defeats the entire point of `stream: true` in the OpenAI request — we'd see the response only after the model finished generating. The streaming path needs SSE chunk parsing as it arrives, which means OkHttp directly.

**Cost:** two HTTP styles in one data source. Mitigated by keeping both in `RemoteChatDataSource` and exposing them through `ChatRepository` so the rest of the app doesn't see the seam.

---

## ADR-005 · `Resource<T>` over three `LiveData` streams

**Date:** 2026-03
**Status:** Accepted

```java
public abstract class Resource<T> {
    public static <T> Resource<T> loading()       { … }
    public static <T> Resource<T> success(T data) { … }
    public static <T> Resource<T> error(String m) { … }
}
```

A single `LiveData<Resource<T>>` per stream, sealed-style state.

**Why:** three booleans (`isLoading`, `error`, `data`) admit invalid combinations (loading AND error AND data). The wrapper makes those unrepresentable. Mirrors the same intuition behind iOS's `ViewState<T>` pattern in [voya](https://github.com/Kaustubha-09/voya/blob/main/docs/decisions.md#adr-001--viewstatet-as-the-single-async-state-shape).

---

## ADR-006 · API key loaded from `gradle.properties`, never hardcoded

**Date:** 2026-03
**Status:** Accepted (after a key-leak incident)

`app/build.gradle` reads `OPENAI_API_KEY` from `~/.gradle/gradle.properties` or `System.getenv()`. Empty fallback so the app builds without a key (mock provider becomes the default).

**Why:** an earlier version had the key literal-embedded in `build.gradle`. Anyone with read access to the repo would have it. We caught this before a public push, rotated the key, and migrated to gradle.properties.

**Enforced by:** `.gitignore` excludes `local.properties`, `*.keys`, `secrets.properties`, `api_keys.properties`, `local.gradle.properties`.

---

## ADR-007 · ViewBinding everywhere, no `findViewById`

**Date:** 2026-03
**Status:** Accepted

Every layout reference goes through generated `*Binding` classes. No `findViewById` calls.

**Why:** compile-time null safety, no `ClassCastException` at runtime, type-aware autocomplete. ViewBinding is the lowest-cost replacement for unsafe view lookup.

---

## ADR-008 · Java 17, not Kotlin

**Date:** 2026-03
**Status:** Accepted

`sourceCompatibility VERSION_17`, no Kotlin. The codebase is pure Java.

**Why:** the project started in a Java-heavy course context. Migrating to Kotlin mid-stream would have been a separate refactor, not a feature. Java 17 records, switch expressions, and pattern matching cover most of the syntactic wins.

**When to revisit:** when adding Compose ([roadmap](roadmap.md)). Compose is Kotlin-only.

---

## ADR-009 · Mock provider toggle is in Settings, not a build variant

**Date:** 2026-03
**Status:** Accepted

The Mock Provider toggle lives in `SettingsBottomSheet` and is persisted in `SharedPreferences`. There is no `mockDebug` build variant.

**Why:** a runtime toggle means QA can switch between mock and real-API runs on the same APK. A build variant would require two APKs and two install steps. The runtime cost is one boolean read per request — negligible.
