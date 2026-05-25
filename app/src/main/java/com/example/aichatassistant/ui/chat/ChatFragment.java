package com.example.aichatassistant.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.aichatassistant.R;
import com.example.aichatassistant.databinding.FragmentChatBinding;
import com.example.aichatassistant.ui.chat.adapter.ChatAdapter;
import com.example.aichatassistant.ui.settings.SettingsBottomSheet;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

/**
 * Primary chat screen fragment.
 *
 * Observes {@link ChatViewModel} LiveData and routes changes to:
 *   - ChatAdapter  (message list rendering + streaming token updates)
 *   - Send/Stop button state
 *   - Empty-state visibility
 *   - Error Snackbar with a Retry action
 */
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel       viewModel;
    private ChatAdapter         adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupViewModel();
        setupRecyclerView();
        setupInputArea();
        setupMenu();
        observeViewModel();
    }

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    private void setupToolbar() {
        // Let the Activity host this fragment's toolbar as the ActionBar
        androidx.appcompat.app.AppCompatActivity activity =
                (androidx.appcompat.app.AppCompatActivity) requireActivity();
        activity.setSupportActionBar(binding.toolbar);
    }

    private void setupViewModel() {
        ChatViewModelFactory factory =
                new ChatViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);

        binding.recyclerView.setLayoutManager(lm);
        binding.recyclerView.setAdapter(adapter);
        // Disable default item animator to prevent flicker during streaming
        binding.recyclerView.setItemAnimator(null);
    }

    private void setupInputArea() {
        binding.etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshSendButton(s.toString().trim());
            }
        });

        binding.etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSendOrStop();
                return true;
            }
            return false;
        });

        binding.btnSend.setOnClickListener(v -> handleSendOrStop());
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_chat, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_clear_chat) {
                    viewModel.clearChat();
                    return true;
                }
                if (id == R.id.action_settings) {
                    SettingsBottomSheet.newInstance()
                            .show(getParentFragmentManager(), SettingsBottomSheet.TAG);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // -----------------------------------------------------------------------
    // ViewModel observation
    // -----------------------------------------------------------------------

    private void observeViewModel() {

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages != null ? messages : new ArrayList<>());
            boolean isEmpty = messages == null || messages.isEmpty();
            binding.layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            // DiffUtil can't detect in-place mutations on the same objects, so we
            // manually force a rebind of the streaming row on every token update.
            if (Boolean.TRUE.equals(viewModel.getIsStreaming().getValue())) {
                adapter.notifyStreamingItem();
            }
            scrollToBottom();
        });

        viewModel.getIsTyping().observe(getViewLifecycleOwner(), typing -> {
            boolean active = Boolean.TRUE.equals(typing);
            adapter.setTyping(active);
            if (active) scrollToBottom();
        });

        viewModel.getIsStreaming().observe(getViewLifecycleOwner(), streaming -> {
            boolean active = Boolean.TRUE.equals(streaming);
            if (active) {
                binding.btnSend.setIconResource(R.drawable.ic_stop);
                binding.btnSend.setEnabled(true);
            } else {
                binding.btnSend.setIconResource(R.drawable.ic_send);
                Editable text = binding.etInput.getText();
                refreshSendButton(text != null ? text.toString().trim() : "");
            }
        });

        viewModel.getStreamingId().observe(getViewLifecycleOwner(), id -> {
            adapter.setStreamingId(id);
            adapter.notifyStreamingItem();
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isEmpty()) return;
            Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> viewModel.retryLastMessage())
                    .show();
            viewModel.clearError();
        });
    }

    // -----------------------------------------------------------------------
    // UI helpers
    // -----------------------------------------------------------------------

    private void handleSendOrStop() {
        if (Boolean.TRUE.equals(viewModel.getIsStreaming().getValue())) {
            viewModel.stopStreaming();
        } else {
            Editable editable = binding.etInput.getText();
            String text = editable != null ? editable.toString().trim() : "";
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                binding.etInput.setText("");
            }
        }
    }

    private void refreshSendButton(String inputText) {
        boolean isStreaming = Boolean.TRUE.equals(viewModel.getIsStreaming().getValue());
        // Button is enabled when: currently streaming (acts as Stop), OR input is non-empty
        binding.btnSend.setEnabled(isStreaming || !inputText.isEmpty());
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.recyclerView.post(() ->
                    binding.recyclerView.smoothScrollToPosition(count - 1));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
