package com.example.aichatassistant.ui.chat;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.di.ServiceLocator;
import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.model.MessageStatus;
import com.example.aichatassistant.domain.model.Sender;
import com.example.aichatassistant.domain.repository.ChatRepository;
import com.example.aichatassistant.domain.usecase.ClearChatUseCase;
import com.example.aichatassistant.domain.usecase.SendMessageUseCase;
import com.example.aichatassistant.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the chat screen.
 *
 * Observable state:
 *   messages    — ordered list driving the RecyclerView
 *   isStreaming — true while the AI is generating a response
 *   isTyping    — true for the brief period before first token arrives
 *   errorEvent  — one-shot string for Snackbar display (cleared after consumption)
 *   streamingId — id of the message row currently receiving tokens
 *
 * Threading model:
 *   - History load: background ExecutorService → main thread via Handler
 *   - Streaming callbacks: always delivered on main thread by data sources
 *   - DB writes: delegated to repository's internal executor
 */
public class ChatViewModel extends AndroidViewModel {

    // --- Observable state --------------------------------------------------
    private final MutableLiveData<List<ChatMessage>> messages    = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean>           isStreaming = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>           isTyping    = new MutableLiveData<>(false);
    private final MutableLiveData<String>            errorEvent  = new MutableLiveData<>(null);
    private final MutableLiveData<String>            streamingId = new MutableLiveData<>(null);

    // --- Dependencies ------------------------------------------------------
    private final SendMessageUseCase sendMessageUseCase;
    private final ClearChatUseCase   clearChatUseCase;
    private final ChatRepository     repository;

    // --- Internal threading ------------------------------------------------
    private final ExecutorService ioExecutor  = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public ChatViewModel(@NonNull Application application) {
        super(application);
        ServiceLocator sl  = ServiceLocator.get();
        sendMessageUseCase = sl.getSendMessageUseCase();
        clearChatUseCase   = sl.getClearChatUseCase();
        repository         = sl.getChatRepository();
        loadHistory();
    }

    // --- Public LiveData accessors -----------------------------------------

    public LiveData<List<ChatMessage>> getMessages()    { return messages; }
    public LiveData<Boolean>           getIsStreaming() { return isStreaming; }
    public LiveData<Boolean>           getIsTyping()    { return isTyping; }
    public LiveData<String>            getErrorEvent()  { return errorEvent; }
    public LiveData<String>            getStreamingId() { return streamingId; }

    // --- Actions -----------------------------------------------------------

    public void sendMessage(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        // Guard: require network when using the real provider
        if (!AppConfig.USE_MOCK_PROVIDER && !NetworkUtils.isConnected(getApplication())) {
            errorEvent.setValue("No internet connection. Check your network and try again.");
            return;
        }

        // 1. Append the user message immediately so the UI feels responsive
        ChatMessage userMsg = new ChatMessage(trimmed, Sender.USER, MessageStatus.COMPLETE);
        appendToList(userMsg);
        repository.saveMessage(userMsg);

        // 2. Add a placeholder AI message in STREAMING state
        ChatMessage aiMsg = new ChatMessage("", Sender.AI, MessageStatus.STREAMING);
        appendToList(aiMsg);

        isStreaming.setValue(true);
        isTyping.setValue(true);
        streamingId.setValue(aiMsg.getId());

        // 3. Fire the use-case with a snapshot of the current history
        List<ChatMessage> history = new ArrayList<>(
                messages.getValue() != null ? messages.getValue() : new ArrayList<>());

        sendMessageUseCase.execute(history, new ChatRepository.StreamingCallback() {

            @Override
            public void onToken(String token) {
                // First token: hide the typing indicator
                if (Boolean.TRUE.equals(isTyping.getValue())) {
                    isTyping.setValue(false);
                }
                appendTokenToMessage(aiMsg.getId(), token);
            }

            @Override
            public void onComplete(String fullContent) {
                finalizeMessage(aiMsg.getId(), fullContent, MessageStatus.COMPLETE);
                repository.saveMessage(aiMsg);
            }

            @Override
            public void onError(String errorMessage) {
                finalizeMessage(aiMsg.getId(),
                        "Sorry, something went wrong. Please try again.",
                        MessageStatus.FAILED);
                errorEvent.setValue(errorMessage);
            }
        });
    }

    /** Cancels in-flight streaming and marks the partial AI message as complete. */
    public void stopStreaming() {
        sendMessageUseCase.cancel();

        String currentId = streamingId.getValue();
        if (currentId == null) return;

        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        for (ChatMessage m : current) {
            if (m.getId().equals(currentId) && m.getStatus() == MessageStatus.STREAMING) {
                String partial = m.getContent().isEmpty() ? "[Stopped]" : m.getContent();
                finalizeMessage(currentId, partial, MessageStatus.COMPLETE);
                repository.saveMessage(m);
                break;
            }
        }
    }

    public void clearChat() {
        messages.setValue(new ArrayList<>());
        clearChatUseCase.execute();
        isStreaming.setValue(false);
        isTyping.setValue(false);
        streamingId.setValue(null);
    }

    /** Re-sends the last user message (used by the Snackbar "Retry" action). */
    public void retryLastMessage() {
        List<ChatMessage> current = messages.getValue();
        if (current == null || current.isEmpty()) return;

        // Find the last user message
        ChatMessage lastUser = null;
        for (int i = current.size() - 1; i >= 0; i--) {
            if (current.get(i).isUser()) {
                lastUser = current.get(i);
                break;
            }
        }
        if (lastUser == null) return;

        // Remove the failed AI message if present
        List<ChatMessage> trimmed = new ArrayList<>(current);
        ChatMessage lastMsg = trimmed.get(trimmed.size() - 1);
        if (lastMsg.isAi() && lastMsg.getStatus() == MessageStatus.FAILED) {
            trimmed.remove(trimmed.size() - 1);
            messages.setValue(trimmed);
        }

        sendMessage(lastUser.getContent());
    }

    public void clearError() {
        errorEvent.setValue(null);
    }

    // --- Private helpers ---------------------------------------------------

    private void loadHistory() {
        ioExecutor.execute(() -> {
            List<ChatMessage> history = repository.loadHistory();
            mainHandler.post(() -> messages.setValue(history));
        });
    }

    private void appendToList(ChatMessage message) {
        List<ChatMessage> current = new ArrayList<>();
        if (messages.getValue() != null) current.addAll(messages.getValue());
        current.add(message);
        messages.setValue(current);
    }

    /**
     * Appends a streaming token to the message identified by {@code id}.
     * Posts a new list reference so LiveData observers fire,
     * but avoids recreating the entire list unnecessarily.
     */
    private void appendTokenToMessage(String id, String token) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        for (ChatMessage m : current) {
            if (m.getId().equals(id)) {
                m.setContent(m.getContent() + token);
                messages.setValue(new ArrayList<>(current));
                return;
            }
        }
    }

    private void finalizeMessage(String id, String content, MessageStatus status) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        for (ChatMessage m : current) {
            if (m.getId().equals(id)) {
                m.setContent(content);
                m.setStatus(status);
                break;
            }
        }

        messages.setValue(new ArrayList<>(current));
        isStreaming.setValue(false);
        isTyping.setValue(false);
        streamingId.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
        sendMessageUseCase.cancel();
    }
}
