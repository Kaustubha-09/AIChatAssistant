package com.example.aichatassistant.domain.repository;

import com.example.aichatassistant.domain.model.ChatMessage;
import java.util.List;

/**
 * Repository contract owned by the domain layer.
 * The data layer implements this; domain UseCases depend only on this interface,
 * making the architecture testable and provider-agnostic.
 */
public interface ChatRepository {

    interface StreamingCallback {
        /** Called for each incremental token. Always delivered on the main thread. */
        void onToken(String token);

        /** Called once the stream is fully complete. Always on the main thread. */
        void onComplete(String fullContent);

        /** Called when an error stops the stream. Always on the main thread. */
        void onError(String errorMessage);
    }

    /**
     * Send the conversation history to the AI provider and stream the response
     * back via {@link StreamingCallback}.
     *
     * @param messages full ordered history (oldest first, newest last)
     * @param callback receives stream events on the main thread
     */
    void sendMessage(List<ChatMessage> messages, StreamingCallback callback);

    /** Cancel any in-flight streaming request immediately. */
    void cancelStreaming();

    /** Persist a message to local Room database on a background thread. */
    void saveMessage(ChatMessage message);

    /**
     * Load all persisted messages, ordered oldest-first.
     * Must be called from a background thread.
     */
    List<ChatMessage> loadHistory();

    /** Delete all persisted messages from the local database. */
    void clearHistory();
}
