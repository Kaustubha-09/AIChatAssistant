package com.example.aichatassistant.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/** Factory required because ChatViewModel extends AndroidViewModel and needs an Application. */
public class ChatViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {

    private final Application application;

    public ChatViewModelFactory(@NonNull Application application) {
        super(application);
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(application);
        }
        return super.create(modelClass);
    }
}
