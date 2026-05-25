package com.example.aichatassistant.domain.usecase;

import com.example.aichatassistant.domain.repository.ChatRepository;

public class ClearChatUseCase {

    private final ChatRepository repository;

    public ClearChatUseCase(ChatRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        repository.clearHistory();
    }
}
