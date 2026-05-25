package com.example.aichatassistant.domain.model;

public enum MessageStatus {
    /** User message being dispatched to the network. */
    SENDING,
    /** AI is actively streaming tokens into this message. */
    STREAMING,
    /** Message fully delivered and finalised. */
    COMPLETE,
    /** An error occurred; the message content may be partial. */
    FAILED
}
