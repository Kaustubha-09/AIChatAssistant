# Limitations

Honest scope. This is a focused single-screen chat client, intentionally narrow.

## Feature scope

- **One screen.** No conversation list, no multi-chat drawer. The roadmap calls out this addition as the next big step.
- **No markdown rendering.** Code blocks, lists, inline code, links render as plain text. Markdown is roadmap.
- **No image input.** Vision models are not wired up. Text-only.
- **No conversation export.** Cannot save / share a chat as markdown.
- **No tablet layout.** Phone-portrait only; the chat consumes the full width.
- **No theme toggle.** `Theme.Material3.Light.NoActionBar` only — no dark mode option, no system-following palette switch.

## Streaming

- **SSE only.** OpenAI's `stream: true` SSE response is the only streaming protocol supported. Endpoints that stream over WebSocket or custom binary protocols would need a different data source.
- **No partial-render cancellation.** Once the SSE stream is open, we let it finish. There's no "stop generating" button.

## Persistence

- **Single Room table.** Every chat is one flat `messages` table. There is no `chats` table grouping turns by conversation — because there's only one conversation today.
- **No multi-device sync.** Chats are device-local. There's no cloud backup.
- **No encryption at rest.** Room's SQLite database is unencrypted. The `AndroidX Security` library (`EncryptedSharedPreferences`, `EncryptedFile`) is a roadmap item.

## Security

- **API key in `SharedPreferences`.** When the user pastes a key at runtime, it lives in `SharedPreferences` — not encrypted. Anyone with root access to the device can read it.
- **API key in `gradle.properties`.** The build-time key path is `~/.gradle/gradle.properties`. Fine for local dev; CI / production deployments need a different secret-management path.
- **No certificate pinning.** OpenAI's TLS is trusted via the system trust store only.

## Networking

- **No retries on transient failures.** A timeout or 503 surfaces as an `Error` immediately. Production retry-with-exponential-backoff is roadmap.
- **No request cancellation on Fragment destruction.** An in-flight stream continues to consume tokens even if the user navigates away. The `ChatViewModel` should cancel its `Call` in `onCleared()` — tracked as a small follow-up.
- **No rate-limit handling.** A 429 response surfaces as an error string; we don't parse `Retry-After` headers.

## Testing

- **Unit tests are sparse.** The codebase has a working test infra (JUnit 4 + Espresso) but only a handful of tests today. Real coverage of `PromptBuilder`, `Resource<T>` mapping, and `MockStreamingDataSource` is a follow-up.
- **No instrumented UI tests.** Espresso flows for the streaming-indicator animation, settings-bottom-sheet interaction, and clear-chat overflow menu are unwritten.

## Build / DevX

- **No Hilt.** Manual `ServiceLocator` means testing requires `@VisibleForTesting` shim methods to swap dependencies. Fine at this scale, friction at scale.
- **No CI before this push.** GitHub Actions workflow added with this commit; prior development was local-only.
- **No automated release pipeline.** APK signing + Play Store upload are manual.
