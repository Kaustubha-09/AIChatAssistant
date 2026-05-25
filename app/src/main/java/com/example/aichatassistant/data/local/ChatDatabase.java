package com.example.aichatassistant.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class}, version = 1, exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {

    private static volatile ChatDatabase instance;

    public abstract MessageDao messageDao();

    public static ChatDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ChatDatabase.class,
                                    "chat_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
