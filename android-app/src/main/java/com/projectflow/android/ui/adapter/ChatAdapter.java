package com.projectflow.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.ChatMessageDto;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;

    private final List<ChatMessageDto> messages = new ArrayList<>();
    private final String currentUserId;

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setMessages(List<ChatMessageDto> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessageDto message) {
        if (message != null) {
            this.messages.add(message);
            notifyItemInserted(this.messages.size() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageDto msg = messages.get(position);
        if ("You".equalsIgnoreCase(msg.senderName) || 
            (currentUserId.length() > 0 && currentUserId.equals(msg.senderId))) {
            return VIEW_TYPE_OUTGOING;
        }
        return VIEW_TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OUTGOING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_outgoing, parent, false);
            return new OutgoingViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_incoming, parent, false);
            return new IncomingViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageDto msg = messages.get(position);
        String timeStr = formatTime(msg.createdAt);

        if (holder instanceof OutgoingViewHolder) {
            OutgoingViewHolder vh = (OutgoingViewHolder) holder;
            vh.tvContent.setText(msg.content);
            vh.tvTimestamp.setText(timeStr);
        } else if (holder instanceof IncomingViewHolder) {
            IncomingViewHolder vh = (IncomingViewHolder) holder;
            vh.tvSender.setText(msg.senderName != null ? msg.senderName : "Colleague");
            vh.tvContent.setText(msg.content);
            vh.tvTimestamp.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTime(String isoString) {
        if (isoString == null || isoString.length() < 16) return "Just now";
        try {
            return isoString.substring(11, 16);
        } catch (Exception e) {
            return "Today";
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvContent, tvTimestamp;
        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSenderName);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTimestamp;
        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
