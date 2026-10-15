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
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnReactionClickListener {
        void onReaction(ChatMessageDto message, String emoji);
    }

    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;

    private final List<ChatMessageDto> messages = new ArrayList<>();
    private final String currentUserId;
    private OnReactionClickListener reactionClickListener;

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void setOnReactionClickListener(OnReactionClickListener listener) {
        this.reactionClickListener = listener;
    }

    public void setMessages(List<ChatMessageDto> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public boolean setMessagesSilently(List<ChatMessageDto> newMessages) {
        if (newMessages == null) return false;
        boolean countChanged = (newMessages.size() != this.messages.size());
        if (countChanged) {
            this.messages.clear();
            this.messages.addAll(newMessages);
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    public void addMessage(ChatMessageDto message) {
        if (message != null) {
            this.messages.add(message);
            notifyItemInserted(this.messages.size() - 1);
        }
    }

    public void updateMessage(ChatMessageDto updated) {
        if (updated == null) return;
        for (int i = 0; i < messages.size(); i++) {
            if (updated.id != null && updated.id.equals(messages.get(i).id)) {
                messages.set(i, updated);
                notifyItemChanged(i);
                break;
            }
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
        Map<String, Integer> reactions = msg.reactions;

        int likeCount = reactions != null && reactions.containsKey("👍") ? reactions.get("👍") : 0;
        int rocketCount = reactions != null && reactions.containsKey("🚀") ? reactions.get("🚀") : 0;
        int fireCount = reactions != null && reactions.containsKey("🔥") ? reactions.get("🔥") : 0;
        int loveCount = reactions != null && reactions.containsKey("❤️") ? reactions.get("❤️") : 0;

        if (holder instanceof OutgoingViewHolder) {
            OutgoingViewHolder vh = (OutgoingViewHolder) holder;
            vh.tvContent.setText(msg.content);
            vh.tvTimestamp.setText(timeStr);

            vh.btnReactLike.setText("👍 " + likeCount);
            vh.btnReactRocket.setText("🚀 " + rocketCount);
            vh.btnReactFire.setText("🔥 " + fireCount);

            bindReactionClick(vh.btnReactLike, msg, "👍");
            bindReactionClick(vh.btnReactRocket, msg, "🚀");
            bindReactionClick(vh.btnReactFire, msg, "🔥");

        } else if (holder instanceof IncomingViewHolder) {
            IncomingViewHolder vh = (IncomingViewHolder) holder;
            vh.tvSender.setText(msg.senderName != null ? msg.senderName : "Colleague");
            vh.tvContent.setText(msg.content);
            vh.tvTimestamp.setText(timeStr);

            vh.btnReactLike.setText("👍 " + likeCount);
            vh.btnReactRocket.setText("🚀 " + rocketCount);
            vh.btnReactFire.setText("🔥 " + fireCount);
            if (vh.btnReactLove != null) vh.btnReactLove.setText("❤️ " + loveCount);

            bindReactionClick(vh.btnReactLike, msg, "👍");
            bindReactionClick(vh.btnReactRocket, msg, "🚀");
            bindReactionClick(vh.btnReactFire, msg, "🔥");
            if (vh.btnReactLove != null) bindReactionClick(vh.btnReactLove, msg, "❤️");
        }
    }

    private void bindReactionClick(View btn, ChatMessageDto msg, String emoji) {
        btn.setOnClickListener(v -> {
            v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }).start();
            if (reactionClickListener != null) {
                reactionClickListener.onReaction(msg, emoji);
            }
        });
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
        TextView btnReactLike, btnReactRocket, btnReactFire, btnReactLove;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSenderName);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnReactLike = itemView.findViewById(R.id.btnReactLike);
            btnReactRocket = itemView.findViewById(R.id.btnReactRocket);
            btnReactFire = itemView.findViewById(R.id.btnReactFire);
            btnReactLove = itemView.findViewById(R.id.btnReactLove);
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTimestamp;
        TextView btnReactLike, btnReactRocket, btnReactFire;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnReactLike = itemView.findViewById(R.id.btnReactLike);
            btnReactRocket = itemView.findViewById(R.id.btnReactRocket);
            btnReactFire = itemView.findViewById(R.id.btnReactFire);
        }
    }
}
