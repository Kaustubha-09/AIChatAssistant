package com.example.aichatassistant;

import android.app.Application;
import com.example.aichatassistant.di.ServiceLocator;

/**
 * Application class — initialises the ServiceLocator (manual DI root)
 * so all singletons are created once and shared across the app lifecycle.
 */
public class AIChatApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.init(this);
    }
}
