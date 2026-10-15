package com.projectflow.android.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.ChatMessageDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.adapter.ChatAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private EditText etChatMessage;
    private FrameLayout btnSendMessage;
    private LinearLayout llEmptyChat;
    private LinearLayout llChannelsContainer;
    private TextView tvActiveChannelTitle, tvActiveChannelSubtitle, tvEmptyChannelName;
    private ProjectFlowRepository repository;
    private String currentUserName = "Rahul Verma";
    private String activeChannel = "general";
    private final List<String> channelList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Rahul Verma");

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        llEmptyChat = view.findViewById(R.id.llEmptyChat);
        llChannelsContainer = view.findViewById(R.id.llChannelsContainer);
        tvActiveChannelTitle = view.findViewById(R.id.tvActiveChannelTitle);
        tvActiveChannelSubtitle = view.findViewById(R.id.tvActiveChannelSubtitle);
        tvEmptyChannelName = view.findViewById(R.id.tvEmptyChannelName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(currentUserName);
        chatAdapter.setOnReactionClickListener((message, emoji) -> {
            repository.reactToMessage(message.id, emoji, new ProjectFlowRepository.DataCallback<ChatMessageDto>() {
                @Override
                public void onData(ChatMessageDto data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> chatAdapter.updateMessage(data));
                    }
                }

                @Override
                public void onError(String message) {}
            });
        });
        rvChatMessages.setAdapter(chatAdapter);

        // Load channels and messages
        loadChannels();

        btnSendMessage.setOnClickListener(v -> {
            String text = etChatMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            etChatMessage.setText("");
            btnSendMessage.setEnabled(false);

            repository.sendChatMessage(text, currentUserName, activeChannel, new ProjectFlowRepository.DataCallback<ChatMessageDto>() {
                @Override
                public void onData(ChatMessageDto data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSendMessage.setEnabled(true);
                            chatAdapter.addMessage(data);
                            llEmptyChat.setVisibility(View.GONE);
                            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSendMessage.setEnabled(true);
                            Toast.makeText(requireContext(), "Failed to deliver message", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        });
    }

    private void loadChannels() {
        repository.fetchChatChannels(new ProjectFlowRepository.DataCallback<List<String>>() {
            @Override
            public void onData(List<String> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        channelList.clear();
                        if (data != null && !data.isEmpty()) {
                            channelList.addAll(data);
                        } else {
                            channelList.addAll(Arrays.asList("general", "engineering", "design-squad", "mobile-dev-squad"));
                        }
                        renderChannelChips();
                        selectChannel(channelList.get(0));
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        channelList.clear();
                        channelList.addAll(Arrays.asList("general", "engineering", "design-squad", "mobile-dev-squad"));
                        renderChannelChips();
                        selectChannel("general");
                    });
                }
            }
        });
    }

    private void renderChannelChips() {
        llChannelsContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (String ch : channelList) {
            TextView chip = new TextView(requireContext());
            chip.setText("#" + ch);
            chip.setTextSize(12);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding((int) (14 * density), (int) (6 * density), (int) (14 * density), (int) (6 * density));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd((int) (8 * density));
            chip.setLayoutParams(lp);

            if (ch.equals(activeChannel)) {
                chip.setBackgroundResource(R.drawable.bg_neu_pill_active);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                chip.setBackgroundResource(R.drawable.bg_neu_pill);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            }

            chip.setOnClickListener(v -> selectChannel(ch));
            llChannelsContainer.addView(chip);
        }
    }

    private void selectChannel(String channel) {
        this.activeChannel = channel;
        tvActiveChannelTitle.setText("#" + channel + " • Team Discussion");
        tvActiveChannelSubtitle.setText("Group discussion for #" + channel);
        tvEmptyChannelName.setText("No messages yet in #" + channel);
        renderChannelChips();
        loadMessagesForChannel(channel);
    }

    private void loadMessagesForChannel(String channel) {
        repository.fetchChatMessagesByChannel(channel, new ProjectFlowRepository.DataCallback<List<ChatMessageDto>>() {
            @Override
            public void onData(List<ChatMessageDto> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (data != null && !data.isEmpty()) {
                            llEmptyChat.setVisibility(View.GONE);
                            chatAdapter.setMessages(data);
                            rvChatMessages.scrollToPosition(data.size() - 1);
                        } else {
                            chatAdapter.setMessages(new ArrayList<>());
                            llEmptyChat.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        llEmptyChat.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }
}
