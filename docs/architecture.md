# Architecture

A focused single-Activity / single-Fragment Android chat app organized in three layers (UI · Domain · Data) with manual dependency injection via a `ServiceLocator` singleton.

## Layer diagram

```
┌────────────────────────────────────────────────────────────┐
│                       UI Layer                             │
│   MainActivity (host) · ChatFragment · ChatAdapter         │
│   ChatViewModel · ChatViewModelFactory                     │
│   SettingsBottomSheet                                      │
└────────────────────────────────────────────────────────────┘
                       ▲ LiveData<Resource<List<Message>>>
                       │
┌────────────────────────────────────────────────────────────┐
│                     Domain Layer                           │
│   ChatRepository (interface)                               │
│   Use cases:                                               │
│     SendMessageUseCase · ObserveMessagesUseCase ·          │
│     ClearChatUseCase                                       │
│   Message (domain model)                                   │
└────────────────────────────────────────────────────────────┘
                       ▲ Java method calls
                       │
┌────────────────────────────────────────────────────────────┐
│                      Data Layer                            │
│   ChatRepositoryImpl                                       │
│   ├─ RemoteChatDataSource (Retrofit + OkHttp + SSE)        │
│   ├─ MockStreamingDataSource (offline streaming sim)       │
│   └─ ChatDatabase (Room)                                   │
│       MessageDao · MessageEntity                           │
│   ChatRequest · ChatResponse · ChatChoice (DTOs)           │
└────────────────────────────────────────────────────────────┘
                       ▲
                       │
┌────────────────────────────────────────────────────────────┐
│                    Cross-cutting                           │
│   AIChatApp (Application) — initializes ServiceLocator     │
│   ServiceLocator (manual DI)                               │
│   AppConfig · Resource<T> · NetworkUtils                   │
│   PromptBuilder · DateUtils                                │
└────────────────────────────────────────────────────────────┘
```

## Three rules every file obeys

1. **Fragment / Activity contains presentation logic only.** No Retrofit calls, no Room access, no JSON parsing. UI talks to `ChatViewModel`; `ChatViewModel` talks to use cases.
2. **`ChatRepository` is the single boundary** between UI and data. The UI never knows whether the response came from `RemoteChatDataSource` (real OpenAI) or `MockStreamingDataSource` (offline simulator) — `ChatRepositoryImpl` picks based on the runtime mock-provider flag and the API-key presence.
3. **Brand decisions live in `res/values/colors.xml` and `themes.xml`** — no `Color.parseColor("#…")` literals in code.

## Streaming pipeline

```
UI                   ChatViewModel                  ChatRepository                 Data source
 │                       │                              │                              │
 │  sendMessage(text)    │                              │                              │
 ├──────────────────────►│                              │                              │
 │                       │  send(message)               │                              │
 │                       ├─────────────────────────────►│                              │
 │                       │                              │  open SSE / mock stream      │
 │                       │                              ├─────────────────────────────►│
 │                       │                              │  Resource.Loading            │
 │  Resource.Loading     │                              │◄─────────────────────────────┤
 │◄──────────────────────┤                              │                              │
 │                       │                              │  (token chunks ─►)           │
 │                       │                              │◄─────────────────────────────┤
 │  partial text updates │                              │                              │
 │◄──────────────────────┤                              │                              │
 │                       │                              │  Resource.Success(text)      │
 │                       │                              │◄─────────────────────────────┤
 │  Resource.Success     │  persist via MessageDao      │                              │
 │◄──────────────────────┤  observe MessageDao to       │                              │
 │                       │  refresh history list        │                              │
```

`Resource<T>` is a sealed-style wrapper with three states — `Loading`, `Success<T>`, `Error(message)`. The UI consumes one stream of `Resource<List<Message>>` and renders accordingly; there are no separate `isLoading` booleans floating around.

## Mock provider — design intent

`MockStreamingDataSource` is not a stub returning "Hello!" — it's a real streaming simulator that emits tokens through the same `Resource.Loading → Loading → … → Success` pipeline as `RemoteChatDataSource`. The UI's typing-indicator animation, partial-render, and final-collapse all exercise on the mock path. Toggling Mock Provider in Settings:

- Costs nothing (no API call).
- Requires no network.
- Drives every UI state visible during a real chat.
- Reveals UX bugs that wouldn't show up if mock returned the full string atomically.

## Manual DI via `ServiceLocator`

```java
public final class ServiceLocator {
    private static volatile ServiceLocator INSTANCE;

    private final OkHttpClient   okHttpClient;       // lazy
    private final Retrofit       retrofit;           // lazy
    private final ChatApi        chatApi;            // lazy
    private final ChatDatabase   chatDatabase;       // lazy
    private final ChatRepository chatRepository;     // lazy

    public static ServiceLocator getInstance(Context ctx) { … }
    public ChatRepository chatRepository() { … }
    public ChatViewModelFactory chatViewModelFactory() { … }
}
```

No Hilt, no Dagger. The app's dependency graph is small enough that manual DI is faster to read and faster to debug. A swap to Hilt would buy us nothing at this scale and add a code-gen build step.

## Streaming over OkHttp

`RemoteChatDataSource` opens an OkHttp call against `/chat/completions` with `stream: true` in the request body. The SSE payload is parsed line-by-line — each `data: { ... }` chunk becomes a partial-token emission, the final `data: [DONE]` closes the stream.

We do **not** use Retrofit's `Call.execute()` for the streaming case — Retrofit's response body returns a fully-buffered `String`, which defeats the purpose. The streaming code path uses OkHttp directly; Retrofit is the request-builder for non-streaming requests.

## Room schema

A single table:

```sql
CREATE TABLE messages (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    role          TEXT NOT NULL,           -- 'user' | 'assistant' | 'system'
    content       TEXT NOT NULL,
    timestamp     INTEGER NOT NULL          -- epoch millis
);
```

`MessageDao` exposes `LiveData<List<MessageEntity>> observeAll()` so the UI refreshes automatically on insert. `clear()` empties the table.

## What runs where

| Concern | Lives in |
|---|---|
| UI rendering | `ChatFragment`, `ChatAdapter`, `SettingsBottomSheet` |
| State holding | `ChatViewModel` |
| Use-case coordination | `SendMessageUseCase`, `ObserveMessagesUseCase`, `ClearChatUseCase` |
| Real API call + SSE parsing | `RemoteChatDataSource` |
| Offline simulation | `MockStreamingDataSource` |
| Repository wiring | `ChatRepositoryImpl` |
| Local persistence | `ChatDatabase`, `MessageDao`, `MessageEntity` |
| Network check | `NetworkUtils` |
| Prompt composition | `PromptBuilder` |
| Manual DI | `ServiceLocator` |
| App-level config | `AppConfig`, `BuildConfig` |
