package com.example.aichatassistant.domain.model;

import java.util.UUID;

/**
 * Core domain model for a single chat message.
 * Lives purely in the domain layer — no Android or Room dependencies here.
 */
public class ChatMessage {

    private final String id;
    private String       content;
    private final Sender sender;
    private MessageStatus status;
    private final long   timestamp;

    /** Constructor for new messages (generates a fresh UUID and current timestamp). */
    public ChatMessage(String content, Sender sender, MessageStatus status) {
        this.id        = UUID.randomUUID().toString();
        this.content   = content;
        this.sender    = sender;
        this.status    = status;
        this.timestamp = System.currentTimeMillis();
    }

    /** Constructor for messages restored from the local database (preserves original id/timestamp). */
    public ChatMessage(String id, String content, Sender sender, MessageStatus status, long timestamp) {
        this.id        = id;
        this.content   = content;
        this.sender    = sender;
        this.status    = status;
        this.timestamp = timestamp;
    }

    public String        getId()        { return id; }
    public String        getContent()   { return content; }
    public Sender        getSender()    { return sender; }
    public MessageStatus getStatus()    { return status; }
    public long          getTimestamp() { return timestamp; }

    public void setContent(String content)      { this.content = content; }
    public void setStatus(MessageStatus status) { this.status  = status; }

    public boolean isUser() { return sender == Sender.USER; }
    public boolean isAi()   { return sender == Sender.AI; }
}
