package com.example.aichatassistant.ui.chat.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichatassistant.R;
import com.example.aichatassistant.domain.model.ChatMessage;
import com.example.aichatassistant.domain.model.MessageStatus;
import com.example.aichatassistant.domain.model.Sender;
import com.example.aichatassistant.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the chat message list.
 *
 * View types:
 *   VIEW_TYPE_USER   (0) — right-aligned blue bubble
 *   VIEW_TYPE_AI     (1) — left-aligned surface bubble
 *   VIEW_TYPE_TYPING (2) — animated three-dot indicator (shown while AI is composing)
 *
 * Update strategy:
 *   setMessages()         → DiffUtil for efficient full-list diffing
 *   notifyStreamingItem() → targeted notifyItemChanged() during live token streaming
 *                           (avoids expensive DiffUtil on every token)
 *
 * Long-press on any message copies its text to the clipboard.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER   = 0;
    private static final int VIEW_TYPE_AI     = 1;
    private static final int VIEW_TYPE_TYPING = 2;

    private List<ChatMessage> messages    = new ArrayList<>();
    private boolean           isTyping    = false;
    private String            streamingId = null;

    // -----------------------------------------------------------------------
    // Public update API
    // -----------------------------------------------------------------------

    public void setMessages(List<ChatMessage> newMessages) {
        DiffUtil.DiffResult result =
                DiffUtil.calculateDiff(new MessageDiffCallback(messages, newMessages));
        messages = new ArrayList<>(newMessages);
        result.dispatchUpdatesTo(this);
    }

    public void setTyping(boolean typing) {
        if (this.isTyping == typing) return;
        this.isTyping = typing;
        if (typing) {
            notifyItemInserted(getItemCount() - 1);
        } else {
            notifyItemRemoved(getItemCount());
        }
    }

    public void setStreamingId(String id) {
        this.streamingId = id;
    }

    /** Call after each incoming token to refresh only the streaming message row. */
    public void notifyStreamingItem() {
        if (streamingId == null) return;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(streamingId)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    // -----------------------------------------------------------------------
    // RecyclerView.Adapter overrides
    // -----------------------------------------------------------------------

    @Override
    public int getItemViewType(int position) {
        if (isTyping && position == messages.size()) return VIEW_TYPE_TYPING;
        return messages.get(position).getSender() == Sender.USER
                ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @Override
    public int getItemCount() {
        return messages.size() + (isTyping ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_USER:
                return new UserViewHolder(
                        inflater.inflate(R.layout.item_user_message, parent, false));
            case VIEW_TYPE_AI:
                return new AiViewHolder(
                        inflater.inflate(R.layout.item_ai_message, parent, false));
            default:
                return new TypingViewHolder(
                        inflater.inflate(R.layout.item_typing_indicator, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TypingViewHolder) return;
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) ((UserViewHolder) holder).bind(message);
        else if (holder instanceof AiViewHolder) ((AiViewHolder) holder).bind(message);
    }

    // -----------------------------------------------------------------------
    // ViewHolder classes
    // -----------------------------------------------------------------------

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvTimestamp;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent   = itemView.findViewById(R.id.tv_message_content);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        void bind(ChatMessage message) {
            tvContent.setText(message.getContent());
            tvTimestamp.setText(DateUtils.formatTimestamp(message.getTimestamp()));
            itemView.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), message.getContent());
                return true;
            });
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvContent;
        private final TextView tvTimestamp;
        private final View     statusDot;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent   = itemView.findViewById(R.id.tv_message_content);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            statusDot   = itemView.findViewById(R.id.view_status_indicator);
        }

        void bind(ChatMessage message) {
            tvContent.setText(message.getContent());

            if (message.getStatus() == MessageStatus.STREAMING) {
                tvTimestamp.setText("Generating\u2026");
            } else {
                tvTimestamp.setText(DateUtils.formatTimestamp(message.getTimestamp()));
            }

            if (statusDot != null) {
                statusDot.setVisibility(
                        message.getStatus() == MessageStatus.FAILED
                                ? View.VISIBLE : View.GONE);
            }

            itemView.setOnLongClickListener(v -> {
                if (!message.getContent().isEmpty()) {
                    copyToClipboard(v.getContext(), message.getContent());
                }
                return true;
            });
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(@NonNull View itemView) { super(itemView); }
    }

    // -----------------------------------------------------------------------
    // DiffUtil
    // -----------------------------------------------------------------------

    static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        MessageDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int o, int n) {
            return oldList.get(o).getId().equals(newList.get(n).getId());
        }

        @Override
        public boolean areContentsTheSame(int o, int n) {
            ChatMessage old = oldList.get(o);
            ChatMessage nw  = newList.get(n);
            return old.getContent().equals(nw.getContent())
                    && old.getStatus() == nw.getStatus();
        }
    }

    // -----------------------------------------------------------------------
    // Clipboard helper
    // -----------------------------------------------------------------------

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager cm =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("AI Response", text));
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
