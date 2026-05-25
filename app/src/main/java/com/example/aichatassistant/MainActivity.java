package com.example.aichatassistant;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aichatassistant.databinding.ActivityMainBinding;
import com.example.aichatassistant.ui.chat.ChatFragment;

/**
 * Single-activity host. All navigation is Fragment-based.
 * The toolbar is owned by ChatFragment via MenuProvider so it can
 * react to streaming state without coupling to the Activity.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commit();
        }
    }
}
