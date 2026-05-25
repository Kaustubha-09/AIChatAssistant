package com.example.aichatassistant.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Room entity — one row per chat message persisted to the local database. */
@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "sender")
    public String sender;   // Sender enum name: "USER" or "AI"

    @ColumnInfo(name = "status")
    public String status;   // MessageStatus enum name

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public MessageEntity(@NonNull String id, String content,
                         String sender, String status, long timestamp) {
        this.id        = id;
        this.content   = content;
        this.sender    = sender;
        this.status    = status;
        this.timestamp = timestamp;
    }
}
