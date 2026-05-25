package com.example.aichatassistant.domain.usecase;

import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.repository.ChatRepository;
import java.util.List;

/**
 * Encapsulates the "send a message and stream the AI response" workflow.
 * Keeping this in the domain layer makes it testable without Android dependencies.
 */
public class SendMessageUseCase {

    private final ChatRepository repository;

    public SendMessageUseCase(ChatRepository repository) {
        this.repository = repository;
    }

    public void execute(List<ChatMessage> history, ChatRepository.StreamingCallback callback) {
        repository.sendMessage(history, callback);
    }

    public void cancel() {
        repository.cancelStreaming();
    }
}
