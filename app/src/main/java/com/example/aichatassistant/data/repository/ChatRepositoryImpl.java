package com.example.aichatassistant.data.repository;

import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.data.datasource.MockStreamingDataSource;
import com.example.aichatassistant.data.datasource.RemoteChatDataSource;
import com.example.aichatassistant.data.local.ChatDatabase;
import com.example.aichatassistant.data.local.MessageEntity;
import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.model.MessageStatus;
import com.example.aichatassistant.domain.model.Sender;
import com.example.aichatassistant.domain.repository.ChatRepository;
import com.example.aichatassistant.utils.PromptBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concrete implementation of ChatRepository.
 *
 * Routes streaming calls to either the mock provider or the real remote API
 * based on {@link AppConfig#USE_MOCK_PROVIDER}.
 *
 * All Room (database) operations run on a dedicated single-thread executor
 * to avoid StrictMode violations and keep the main thread free.
 */
public class ChatRepositoryImpl implements ChatRepository {

    private final MockStreamingDataSource mockDataSource;
    private final RemoteChatDataSource    remoteDataSource;
    private final ChatDatabase            database;
    private final PromptBuilder           promptBuilder;
    private final ExecutorService         dbExecutor;

    public ChatRepositoryImpl(MockStreamingDataSource mockDataSource,
                               RemoteChatDataSource remoteDataSource,
                               ChatDatabase database,
                               PromptBuilder promptBuilder) {
        this.mockDataSource   = mockDataSource;
        this.remoteDataSource = remoteDataSource;
        this.database         = database;
        this.promptBuilder    = promptBuilder;
        this.dbExecutor       = Executors.newSingleThreadExecutor();
    }

    @Override
    public void sendMessage(List<ChatMessage> messages, StreamingCallback callback) {
        if (AppConfig.USE_MOCK_PROVIDER) {
            mockDataSource.streamResponse(messages, callback);
        } else {
            remoteDataSource.streamCompletion(
                    promptBuilder.buildApiMessages(messages),
                    callback
            );
        }
    }

    @Override
    public void cancelStreaming() {
        if (AppConfig.USE_MOCK_PROVIDER) {
            mockDataSource.cancel();
        } else {
            remoteDataSource.cancel();
        }
    }

    @Override
    public void saveMessage(ChatMessage message) {
        dbExecutor.execute(() -> database.messageDao().insert(toEntity(message)));
    }

    @Override
    public List<ChatMessage> loadHistory() {
        // Must be called from a background thread
        List<MessageEntity> entities = database.messageDao().getAllMessages();
        List<ChatMessage>   messages = new ArrayList<>();
        for (MessageEntity e : entities) {
            messages.add(toDomain(e));
        }
        return messages;
    }

    @Override
    public void clearHistory() {
        dbExecutor.execute(() -> database.messageDao().deleteAll());
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    private MessageEntity toEntity(ChatMessage m) {
        return new MessageEntity(
                m.getId(),
                m.getContent(),
                m.getSender().name(),
                m.getStatus().name(),
                m.getTimestamp()
        );
    }

    private ChatMessage toDomain(MessageEntity e) {
        return new ChatMessage(
                e.id,
                e.content,
                Sender.valueOf(e.sender),
                MessageStatus.valueOf(e.status),
                e.timestamp
        );
    }
}
