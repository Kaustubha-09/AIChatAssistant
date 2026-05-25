package com.example.aichatassistant.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<MessageEntity> getAllMessages();

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    MessageEntity findById(String id);

    @Query("DELETE FROM messages")
    void deleteAll();
}
