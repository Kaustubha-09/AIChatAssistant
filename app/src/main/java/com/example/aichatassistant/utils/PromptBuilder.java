package com.example.aichatassistant.utils;

import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.data.model.ApiMessage;
import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.model.MessageStatus;
import com.example.aichatassistant.domain.model.Sender;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the messages array sent to the LLM API.
 *
 * Responsibilities:
 *  - Prepend the configurable system prompt
 *  - Enforce the sliding context window (HISTORY_WINDOW messages max)
 *  - Map domain ChatMessage → API ApiMessage roles
 *  - Support runtime system-prompt swaps (persona / mode support)
 *
 * This class lives in utils (not domain) because it references the data-layer
 * ApiMessage DTO. In a larger project it would sit in a dedicated 'mapper' package.
 */
public class PromptBuilder {

    private String systemPrompt;

    public PromptBuilder() {
        this.systemPrompt = AppConfig.SYSTEM_PROMPT;
    }

    public void   setSystemPrompt(String prompt) { this.systemPrompt = prompt; }
    public String getSystemPrompt()              { return systemPrompt; }

    /**
     * Convert the conversation history into the API messages array.
     *
     * Structure:
     *   [system message]
     *   [up to HISTORY_WINDOW most-recent user/AI turns]
     */
    public List<ApiMessage> buildApiMessages(List<ChatMessage> history) {
        List<ApiMessage> apiMessages = new ArrayList<>();
        apiMessages.add(ApiMessage.system(systemPrompt));

        int start = Math.max(0, history.size() - AppConfig.HISTORY_WINDOW);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);

            if (msg.getSender() == Sender.USER) {
                apiMessages.add(ApiMessage.user(msg.getContent()));

            } else if (msg.getSender() == Sender.AI
                    && msg.getStatus() == MessageStatus.COMPLETE) {
                // Only include fully-completed AI messages — skip streaming/failed rows
                apiMessages.add(ApiMessage.assistant(msg.getContent()));
            }
        }
        return apiMessages;
    }
}
