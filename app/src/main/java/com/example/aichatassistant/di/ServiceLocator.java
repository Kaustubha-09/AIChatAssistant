package com.example.aichatassistant.di;

import android.content.Context;

import com.example.aichatassistant.data.datasource.MockStreamingDataSource;
import com.example.aichatassistant.data.datasource.RemoteChatDataSource;
import com.example.aichatassistant.data.local.ChatDatabase;
import com.example.aichatassistant.data.repository.ChatRepositoryImpl;
import com.example.aichatassistant.domain.repository.ChatRepository;
import com.example.aichatassistant.domain.usecase.ClearChatUseCase;
import com.example.aichatassistant.domain.usecase.SendMessageUseCase;
import com.example.aichatassistant.utils.PromptBuilder;

/**
 * Manual dependency-injection root (ServiceLocator pattern).
 *
 * Why not Hilt?
 *   Hilt in a pure-Java Android project requires kapt (Kotlin annotation processing),
 *   which adds build complexity and mixed-toolchain confusion for no functional gain
 *   at this scale. ServiceLocator gives the same singleton guarantee with zero magic.
 *
 * Usage:
 *   Call ServiceLocator.init(this) once in AIChatApp.onCreate().
 *   Then inject anywhere via ServiceLocator.get().getSomeService().
 */
public class ServiceLocator {

    private static ServiceLocator instance;

    private final ChatRepository     chatRepository;
    private final SendMessageUseCase sendMessageUseCase;
    private final ClearChatUseCase   clearChatUseCase;
    private final PromptBuilder      promptBuilder;

    private ServiceLocator(Context context) {
        promptBuilder = new PromptBuilder();

        ChatDatabase            db     = ChatDatabase.getInstance(context);
        MockStreamingDataSource mock   = new MockStreamingDataSource();
        RemoteChatDataSource    remote = new RemoteChatDataSource();

        chatRepository     = new ChatRepositoryImpl(mock, remote, db, promptBuilder);
        sendMessageUseCase = new SendMessageUseCase(chatRepository);
        clearChatUseCase   = new ClearChatUseCase(chatRepository);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new ServiceLocator(context.getApplicationContext());
        }
    }

    public static ServiceLocator get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "ServiceLocator not initialised. Call ServiceLocator.init() in Application.onCreate().");
        }
        return instance;
    }

    public ChatRepository     getChatRepository()     { return chatRepository; }
    public SendMessageUseCase getSendMessageUseCase() { return sendMessageUseCase; }
    public ClearChatUseCase   getClearChatUseCase()   { return clearChatUseCase; }
    public PromptBuilder      getPromptBuilder()      { return promptBuilder; }
}
