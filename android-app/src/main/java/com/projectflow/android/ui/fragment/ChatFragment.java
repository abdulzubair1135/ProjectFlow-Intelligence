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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.ChatMessageDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.adapter.ChatAdapter;

import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private EditText etChatMessage;
    private FrameLayout btnSendMessage;
    private LinearLayout llEmptyChat;
    private ProjectFlowRepository repository;
    private String currentUserName = "You";

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
        currentUserName = prefs.getString("user_name", "You");

        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        llEmptyChat = view.findViewById(R.id.llEmptyChat);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(currentUserName);
        rvChatMessages.setAdapter(chatAdapter);

        loadMessages();

        btnSendMessage.setOnClickListener(v -> {
            String text = etChatMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            etChatMessage.setText("");
            btnSendMessage.setEnabled(false);

            repository.sendChatMessage(text, currentUserName, new ProjectFlowRepository.DataCallback<ChatMessageDto>() {
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

    private void loadMessages() {
        repository.fetchChatMessages(new ProjectFlowRepository.DataCallback<List<ChatMessageDto>>() {
            @Override
            public void onData(List<ChatMessageDto> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (data != null && !data.isEmpty()) {
                            llEmptyChat.setVisibility(View.GONE);
                            chatAdapter.setMessages(data);
                            rvChatMessages.scrollToPosition(data.size() - 1);
                        } else {
                            llEmptyChat.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                // Keep local / cached
            }
        });
    }
}
