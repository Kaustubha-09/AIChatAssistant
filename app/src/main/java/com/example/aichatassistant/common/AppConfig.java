package com.example.aichatassistant.common;

import com.example.aichatassistant.BuildConfig;

/**
 * Centralised configuration constants.
 *
 * To go live:
 *   1. Set USE_MOCK_PROVIDER = false
 *   2. Replace OPENAI_API_KEY in app/build.gradle with your real key
 */
public final class AppConfig {

    private AppConfig() {}

    // --- Provider toggle ---------------------------------------------------
    /** true  = simulated word-by-word streaming (no network needed)
     *  false = real OpenAI-compatible HTTP + SSE streaming               */
    public static final boolean USE_MOCK_PROVIDER = false;

    // --- LLM settings ------------------------------------------------------
    public static final String API_KEY       = BuildConfig.OPENAI_API_KEY;
    public static final String BASE_URL      = BuildConfig.OPENAI_BASE_URL;
    public static final String DEFAULT_MODEL = BuildConfig.DEFAULT_MODEL;
    public static final int    MAX_TOKENS    = 2048;
    public static final float  TEMPERATURE   = 0.7f;

    // --- Context window ----------------------------------------------------
    /** Max number of recent messages included in each API request. */
    public static final int HISTORY_WINDOW = 20;

    // --- Mock streaming ----------------------------------------------------
    /** Milliseconds between simulated tokens — controls "typing speed". */
    public static final int MOCK_TOKEN_DELAY_MS = 35;

    // --- Networking --------------------------------------------------------
    public static final int CONNECT_TIMEOUT_SECONDS = 15;
    public static final int READ_TIMEOUT_SECONDS    = 60;
    public static final int WRITE_TIMEOUT_SECONDS   = 15;

    // --- Default system prompt ---------------------------------------------
    public static final String SYSTEM_PROMPT =
            "You are a helpful, knowledgeable AI assistant. " +
            "Respond clearly and concisely. " +
            "When writing code, always use markdown code blocks with the language tag.";
}
