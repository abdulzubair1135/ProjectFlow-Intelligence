package com.projectflow.android.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.projectflow.android.data.model.ConversationItem;
import com.projectflow.android.data.remote.model.DtoModels.ChatGroupDto;
import com.projectflow.android.data.remote.model.DtoModels.ChatMessageDto;
import com.projectflow.android.data.remote.model.DtoModels.TeamMemberDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.adapter.ChatAdapter;
import com.projectflow.android.ui.adapter.ConversationAdapter;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFragment extends Fragment {

    public static ChatFragment newInstance(String targetChannel, String targetName, String targetType, boolean targetOnline) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        if (targetChannel != null) args.putString("target_channel", targetChannel);
        if (targetName != null) args.putString("target_name", targetName);
        if (targetType != null) args.putString("target_type", targetType);
        args.putBoolean("target_online", targetOnline);
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatFragment newInstance(String targetChannel, String targetName, String targetType) {
        return newInstance(targetChannel, targetName, targetType, false);
    }

    public static ChatFragment newInstance(String targetChannel) {
        return newInstance(targetChannel, null, null, false);
    }

    // View Containers
    private View layoutConversationList;
    private View layoutChatRoom;

    // Conversation List Views (WhatsApp Inbox)
    private RecyclerView rvConversations;
    private ConversationAdapter conversationAdapter;
    private View btnCreateGroup;
    private EditText etSearchConversations;
    private TextView tabAllChats, tabDirectChats, tabGroupChats;
    private View llEmptyConversations;

    // Chat Room Views (WhatsApp Conversation)
    private View btnBackToConversations;
    private View btnChatRoomOptions;
    private TextView tvChatRoomTitle, tvChatRoomSubtitle, tvChatRoomInitial, tvChatRoomTypeBadge;
    private ImageView ivChatRoomGroupIcon;
    private View viewChatRoomOnlineDot;
    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private EditText etChatMessage;
    private FrameLayout btnSendMessage;
    private View llEmptyChat;
    private TextView tvEmptyChannelName;

    private final List<TeamMemberDto> cachedTeamMembers = new ArrayList<>();
    private final List<ChatGroupDto> cachedGroups = new ArrayList<>();

    private ProjectFlowRepository repository;
    private SharedPreferences prefs;
    private String currentUserName = "User";
    private String currentUserEmail = "admin@projectflow.io";
    private String currentUserRole = "ADMINISTRATOR";

    private String activeChannel = "general";
    private String activeChatTitle = "General Squad";
    private String activeChatType = "GROUP";

    private final List<ConversationItem> allConversations = new ArrayList<>();
    private final List<ConversationItem> filteredConversations = new ArrayList<>();
    private String currentTab = "ALL";
    private String searchQuery = "";

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());
        prefs = requireContext().getSharedPreferences("projectflow_prefs", Context.MODE_PRIVATE);

        currentUserEmail = prefs.getString("user_email", "admin@projectflow.io");
        String defaultName = currentUserEmail.contains("@") ? currentUserEmail.split("@")[0] : "ProjectFlow User";
        currentUserName = prefs.getString("user_name", prefs.getString("user_display_name", defaultName));
        currentUserRole = prefs.getString("user_role", "ADMINISTRATOR");

        // Bind Containers
        layoutConversationList = view.findViewById(R.id.layoutConversationList);
        layoutChatRoom = view.findViewById(R.id.layoutChatRoom);

        // Bind Inbox Views
        btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        etSearchConversations = view.findViewById(R.id.etSearchConversations);
        tabAllChats = view.findViewById(R.id.tabAllChats);
        tabDirectChats = view.findViewById(R.id.tabDirectChats);
        tabGroupChats = view.findViewById(R.id.tabGroupChats);
        rvConversations = view.findViewById(R.id.rvConversations);
        llEmptyConversations = view.findViewById(R.id.llEmptyConversations);

        btnBackToConversations = view.findViewById(R.id.btnBackToConversations);
        btnChatRoomOptions = view.findViewById(R.id.btnChatRoomOptions);
        if (btnChatRoomOptions != null) {
            btnChatRoomOptions.setOnClickListener(v -> showChatRoomDetails());
        }
        tvChatRoomTitle = view.findViewById(R.id.tvChatRoomTitle);
        tvChatRoomSubtitle = view.findViewById(R.id.tvChatRoomSubtitle);
        tvChatRoomInitial = view.findViewById(R.id.tvChatRoomInitial);
        tvChatRoomTypeBadge = view.findViewById(R.id.tvChatRoomTypeBadge);
        ivChatRoomGroupIcon = view.findViewById(R.id.ivChatRoomGroupIcon);
        viewChatRoomOnlineDot = view.findViewById(R.id.viewChatRoomOnlineDot);
        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        llEmptyChat = view.findViewById(R.id.llEmptyChat);
        tvEmptyChannelName = view.findViewById(R.id.tvEmptyChannelName);

        // Setup RecyclerViews
        conversationAdapter = new ConversationAdapter(item -> {
            openChatRoom(item.id, item.title, item.type == ConversationItem.Type.DIRECT ? "DIRECT" : "GROUP");
        });
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvConversations.setAdapter(conversationAdapter);

        LinearLayoutManager msgLayoutManager = new LinearLayoutManager(requireContext());
        msgLayoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(msgLayoutManager);
        chatAdapter = new ChatAdapter(currentUserName);
        chatAdapter.setOnReactionClickListener((message, emoji) -> {
            repository.reactToMessage(message.id, emoji, new ProjectFlowRepository.DataCallback<ChatMessageDto>() {
                @Override
                public void onData(ChatMessageDto data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> chatAdapter.updateMessage(data));
                    }
                }
                @Override public void onError(String message) {}
            });
        });
        rvChatMessages.setAdapter(chatAdapter);

        // Setup WhatsApp Filter Tabs
        tabAllChats.setOnClickListener(v -> selectTab("ALL", tabAllChats));
        tabDirectChats.setOnClickListener(v -> selectTab("DIRECT", tabDirectChats));
        tabGroupChats.setOnClickListener(v -> selectTab("GROUP", tabGroupChats));

        // Setup Search
        etSearchConversations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applyConversationFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Setup Back Button from Chat Room to Conversation List
        btnBackToConversations.setOnClickListener(v -> {
            layoutChatRoom.setVisibility(View.GONE);
            layoutConversationList.setVisibility(View.VISIBLE);
            loadConversations();
        });

        // Setup Admin Group Creation Action
        btnCreateGroup.setOnClickListener(v -> handleCreateGroupClick());

        // Setup Message Composer
        btnSendMessage.setOnClickListener(v -> sendMessage());

        // Check Arguments: Did we open into a specific chat directly?
        if (getArguments() != null && getArguments().containsKey("target_channel")) {
            String ch = getArguments().getString("target_channel");
            String name = getArguments().getString("target_name", ch);
            String type = getArguments().getString("target_type", ch != null && ch.startsWith("dm_") ? "DIRECT" : "GROUP");
            openChatRoom(ch, name, type);
        } else {
            showInbox();
        }
    }

    private void showInbox() {
        layoutChatRoom.setVisibility(View.GONE);
        layoutConversationList.setVisibility(View.VISIBLE);
        loadConversations();
    }

    public void switchChannel(String channel) {
        String type = (channel != null && channel.startsWith("dm_")) ? "DIRECT" : "GROUP";
        String name = channel;
        openChatRoom(channel, name, type);
    }

    public void openChatRoom(String channel, String title, String type) {
        activeChannel = channel != null ? channel : "general";
        activeChatTitle = title != null ? title : activeChannel;
        activeChatType = type != null ? type : (activeChannel.startsWith("dm_") ? "DIRECT" : "GROUP");

        layoutConversationList.setVisibility(View.GONE);
        layoutChatRoom.setVisibility(View.VISIBLE);

        tvChatRoomTitle.setText(activeChatTitle);

        if ("DIRECT".equalsIgnoreCase(activeChatType)) {
            boolean isOnline = false;
            if (getArguments() != null && getArguments().containsKey("target_online") && activeChannel != null && activeChannel.equals(getArguments().getString("target_channel"))) {
                isOnline = getArguments().getBoolean("target_online", false);
            } else {
                for (ConversationItem conv : allConversations) {
                    if (conv.id != null && conv.id.equalsIgnoreCase(activeChannel)) {
                        isOnline = conv.online;
                        break;
                    }
                }
            }

            if (isOnline) {
                tvChatRoomSubtitle.setText("Online • Direct Chat");
                tvChatRoomSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.earth_success));
                viewChatRoomOnlineDot.setVisibility(View.VISIBLE);
            } else {
                tvChatRoomSubtitle.setText("Offline • Direct Chat");
                tvChatRoomSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                viewChatRoomOnlineDot.setVisibility(View.GONE);
            }
            tvChatRoomTypeBadge.setText("1-ON-1");
            ivChatRoomGroupIcon.setVisibility(View.GONE);
            tvChatRoomInitial.setVisibility(View.VISIBLE);
            String init = activeChatTitle.length() > 0 ? activeChatTitle.substring(0, 1).toUpperCase() : "U";
            tvChatRoomInitial.setText(init);
        } else {
            tvChatRoomSubtitle.setText("Group Squad • Active");
            tvChatRoomSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvChatRoomTypeBadge.setText("SQUAD");
            ivChatRoomGroupIcon.setVisibility(View.VISIBLE);
            tvChatRoomInitial.setVisibility(View.GONE);
            viewChatRoomOnlineDot.setVisibility(View.GONE);
        }

        if (tvEmptyChannelName != null) {
            tvEmptyChannelName.setText("No messages yet in " + activeChatTitle + ".");
        }

        loadMessagesForActiveChannel();
    }

    private void selectTab(String tab, TextView selectedView) {
        currentTab = tab;
        TextView[] tabs = {tabAllChats, tabDirectChats, tabGroupChats};
        for (TextView t : tabs) {
            if (t == selectedView) {
                t.setBackgroundResource(R.drawable.bg_neu_pill_active);
                t.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                t.setBackgroundResource(R.drawable.bg_neu_pill);
                t.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            }
        }
        applyConversationFilter();
    }

    private void loadConversations() {
        // Fetch groups and team members to build WhatsApp conversation list
        repository.fetchChatGroups(new ProjectFlowRepository.DataCallback<List<ChatGroupDto>>() {
            @Override
            public void onData(List<ChatGroupDto> groups) {
                repository.fetchTeam(new ProjectFlowRepository.DataCallback<List<TeamMemberDto>>() {
                    @Override
                    public void onData(List<TeamMemberDto> teamMembers) {
                        repository.fetchAllChatMessages(new ProjectFlowRepository.DataCallback<List<ChatMessageDto>>() {
                            @Override
                            public void onData(List<ChatMessageDto> allMsgs) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            buildConversationList(groups, teamMembers, allMsgs));
                                }
                            }
                            @Override public void onError(String msg) {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            buildConversationList(groups, teamMembers, Collections.emptyList()));
                                }
                            }
                        });
                    }
                    @Override public void onError(String message) {}
                });
            }
            @Override public void onError(String message) {}
        });
    }

    private void buildConversationList(List<ChatGroupDto> groups, List<TeamMemberDto> teamMembers, List<ChatMessageDto> messages) {
        allConversations.clear();
        cachedGroups.clear();
        if (groups != null) cachedGroups.addAll(groups);
        cachedTeamMembers.clear();
        if (teamMembers != null) cachedTeamMembers.addAll(teamMembers);

        Map<String, ChatMessageDto> lastMsgByChannel = new HashMap<>();
        if (messages != null) {
            for (ChatMessageDto m : messages) {
                if (m.channel != null) {
                    lastMsgByChannel.put(m.channel, m);
                }
            }
        }

        // 1. Build Group Conversations
        if (groups != null) {
            for (ChatGroupDto g : groups) {
                ChatMessageDto lastMsg = lastMsgByChannel.get(g.channel);
                String snippet = lastMsg != null ? (lastMsg.senderName + ": " + lastMsg.content) : g.description;
                String time = lastMsg != null ? formatTime(lastMsg.createdAt) : "";
                ConversationItem item = new ConversationItem(
                        g.channel,
                        g.name != null ? g.name : "Squad Group",
                        snippet,
                        time,
                        "GP",
                        ConversationItem.Type.GROUP,
                        false
                );
                item.memberCount = g.members != null ? g.members.size() : 0;
                allConversations.add(item);
            }
        }

        // 2. Build 1-on-1 Direct Conversations from Team Members
        if (teamMembers != null) {
            for (TeamMemberDto m : teamMembers) {
                if (m.email != null && m.email.equalsIgnoreCase(currentUserEmail)) {
                    continue; // Skip self
                }
                String clean1 = currentUserEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                String target = m.email != null ? m.email : m.fullName;
                String clean2 = target.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                String dmChannel = (clean1.compareTo(clean2) < 0)
                        ? "dm_" + clean1 + "_" + clean2
                        : "dm_" + clean2 + "_" + clean1;

                ChatMessageDto lastMsg = lastMsgByChannel.get(dmChannel);
                String snippet = lastMsg != null ? (lastMsg.senderName + ": " + lastMsg.content) : ("Direct chat with " + m.fullName);
                String time = lastMsg != null ? formatTime(lastMsg.createdAt) : "";
                String init = m.fullName != null && m.fullName.length() > 0 ? m.fullName.substring(0, 1).toUpperCase() : "U";

                ConversationItem item = new ConversationItem(
                        dmChannel,
                        m.fullName != null ? m.fullName : m.email,
                        snippet,
                        time,
                        init,
                        ConversationItem.Type.DIRECT,
                        m.isOnline
                );
                item.targetEmail = m.email;
                allConversations.add(item);
            }
        }

        applyConversationFilter();
    }

    private void applyConversationFilter() {
        filteredConversations.clear();
        for (ConversationItem item : allConversations) {
            boolean matchesTab = "ALL".equals(currentTab) ||
                    ("DIRECT".equals(currentTab) && item.type == ConversationItem.Type.DIRECT) ||
                    ("GROUP".equals(currentTab) && item.type == ConversationItem.Type.GROUP);

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (item.title != null && item.title.toLowerCase().contains(searchQuery)) ||
                    (item.snippet != null && item.snippet.toLowerCase().contains(searchQuery));

            if (matchesTab && matchesSearch) {
                filteredConversations.add(item);
            }
        }

        conversationAdapter.setItems(filteredConversations);

        if (filteredConversations.isEmpty()) {
            rvConversations.setVisibility(View.GONE);
            llEmptyConversations.setVisibility(View.VISIBLE);
        } else {
            llEmptyConversations.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
        }
    }

    private void loadMessagesForActiveChannel() {
        repository.fetchChatMessagesByChannel(activeChannel, new ProjectFlowRepository.DataCallback<List<ChatMessageDto>>() {
            @Override
            public void onData(List<ChatMessageDto> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        chatAdapter.setMessages(data);
                        if (data == null || data.isEmpty()) {
                            llEmptyChat.setVisibility(View.VISIBLE);
                        } else {
                            llEmptyChat.setVisibility(View.GONE);
                            rvChatMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
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

    private void sendMessage() {
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
                        Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // =============================================================
    // ADMIN-ONLY GROUP CREATION
    // =============================================================
    private void handleCreateGroupClick() {
        boolean isAdmin = "ADMINISTRATOR".equalsIgnoreCase(currentUserRole) ||
                (currentUserEmail != null && currentUserEmail.toLowerCase().contains("admin"));

        if (!isAdmin) {
            Toast.makeText(requireContext(), "Permission Denied: Only Administrators can create new group squads.", Toast.LENGTH_LONG).show();
            return;
        }

        // Open Admin Group Creation Dialog
        showCreateGroupDialog();
    }

    private void showCreateGroupDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etGroupName = dialogView.findViewById(R.id.etGroupName);
        EditText etGroupDescription = dialogView.findViewById(R.id.etGroupDescription);
        RecyclerView rvGroupMemberSelect = dialogView.findViewById(R.id.rvGroupMemberSelect);
        TextView btnCancelGroup = dialogView.findViewById(R.id.btnCancelGroup);
        Button btnSubmitGroup = dialogView.findViewById(R.id.btnSubmitGroup);

        rvGroupMemberSelect.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load members for checkbox selection
        repository.fetchTeam(new ProjectFlowRepository.DataCallback<List<TeamMemberDto>>() {
            @Override
            public void onData(List<TeamMemberDto> members) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        MemberSelectAdapter adapter = new MemberSelectAdapter(members);
                        rvGroupMemberSelect.setAdapter(adapter);
                    });
                }
            }
            @Override public void onError(String message) {}
        });

        btnCancelGroup.setOnClickListener(v -> dialog.dismiss());

        btnSubmitGroup.setOnClickListener(v -> {
            String name = etGroupName.getText().toString().trim();
            String desc = etGroupDescription.getText().toString().trim();

            if (name.isEmpty()) {
                etGroupName.setError("Group name required");
                return;
            }

            MemberSelectAdapter adapter = (MemberSelectAdapter) rvGroupMemberSelect.getAdapter();
            List<String> selectedEmails = adapter != null ? new ArrayList<>(adapter.getSelectedEmails()) : new ArrayList<>();
            if (!selectedEmails.contains(currentUserEmail)) {
                selectedEmails.add(currentUserEmail);
            }

            btnSubmitGroup.setEnabled(false);
            btnSubmitGroup.setText("Creating...");

            repository.createChatGroup(name, desc, selectedEmails, currentUserRole, currentUserEmail, new ProjectFlowRepository.DataCallback<ChatGroupDto>() {
                @Override
                public void onData(ChatGroupDto newGroup) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            dialog.dismiss();
                            Toast.makeText(requireContext(), "Squad Group \"" + newGroup.name + "\" created successfully!", Toast.LENGTH_SHORT).show();
                            openChatRoom(newGroup.channel, newGroup.name, "GROUP");
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSubmitGroup.setEnabled(true);
                            btnSubmitGroup.setText("Create Group");
                            Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        });

        dialog.show();
    }

    private static class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.ViewHolder> {
        private final List<TeamMemberDto> members;
        private final Set<String> selectedEmails = new HashSet<>();

        MemberSelectAdapter(List<TeamMemberDto> members) {
            this.members = members != null ? members : Collections.emptyList();
        }

        Set<String> getSelectedEmails() {
            return selectedEmails;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_select, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TeamMemberDto m = members.get(position);
            holder.tvName.setText(m.fullName != null ? m.fullName : m.email);
            holder.tvEmailRole.setText(m.email + " • " + m.role);
            holder.cbSelect.setChecked(selectedEmails.contains(m.email));

            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedEmails.add(m.email);
                else selectedEmails.remove(m.email);
            });
            holder.itemView.setOnClickListener(v -> holder.cbSelect.setChecked(!holder.cbSelect.isChecked()));
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSelect;
            TextView tvName, tvEmailRole;

            ViewHolder(@NonNull View v) {
                super(v);
                cbSelect = v.findViewById(R.id.cbMemberSelected);
                tvName = v.findViewById(R.id.tvMemberName);
                tvEmailRole = v.findViewById(R.id.tvMemberEmailRole);
            }
        }
    }

    private void showChatRoomDetails() {
        if ("DIRECT".equalsIgnoreCase(activeChatType)) {
            TeamMemberDto targetMember = null;
            for (TeamMemberDto m : cachedTeamMembers) {
                if (m.fullName != null && m.fullName.equalsIgnoreCase(activeChatTitle)) {
                    targetMember = m;
                    break;
                }
                if (m.email != null) {
                    String clean1 = currentUserEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                    String clean2 = m.email.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                    String dmChannel = (clean1.compareTo(clean2) < 0) ? "dm_" + clean1 + "_" + clean2 : "dm_" + clean2 + "_" + clean1;
                    if (dmChannel.equalsIgnoreCase(activeChannel)) {
                        targetMember = m;
                        break;
                    }
                }
            }

            if (targetMember != null) {
                displayMemberDetailsDialog(targetMember);
            } else {
                repository.fetchTeam(new ProjectFlowRepository.DataCallback<List<TeamMemberDto>>() {
                    @Override
                    public void onData(List<TeamMemberDto> data) {
                        if (data != null) {
                            cachedTeamMembers.clear();
                            cachedTeamMembers.addAll(data);
                            for (TeamMemberDto m : data) {
                                if (m.fullName != null && m.fullName.equalsIgnoreCase(activeChatTitle)) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> displayMemberDetailsDialog(m));
                                    }
                                    return;
                                }
                            }
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayBasicDetailsDialog(activeChatTitle, "1-on-1 Direct Chat"));
                        }
                    }
                    @Override
                    public void onError(String message) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayBasicDetailsDialog(activeChatTitle, "1-on-1 Direct Chat"));
                        }
                    }
                });
            }
        } else {
            ChatGroupDto targetGroup = null;
            for (ChatGroupDto g : cachedGroups) {
                if (g.channel != null && g.channel.equalsIgnoreCase(activeChannel)) {
                    targetGroup = g;
                    break;
                }
            }

            if (targetGroup != null) {
                displayGroupDetailsDialog(targetGroup);
            } else {
                repository.fetchChatGroups(new ProjectFlowRepository.DataCallback<List<ChatGroupDto>>() {
                    @Override
                    public void onData(List<ChatGroupDto> data) {
                        if (data != null) {
                            cachedGroups.clear();
                            cachedGroups.addAll(data);
                            for (ChatGroupDto g : data) {
                                if (g.channel != null && g.channel.equalsIgnoreCase(activeChannel)) {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> displayGroupDetailsDialog(g));
                                    }
                                    return;
                                }
                            }
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayBasicDetailsDialog(activeChatTitle, "Squad Group Chat"));
                        }
                    }
                    @Override
                    public void onError(String message) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayBasicDetailsDialog(activeChatTitle, "Squad Group Chat"));
                        }
                    }
                });
            }
        }
    }

    private void displayMemberDetailsDialog(TeamMemberDto member) {
        new AlertDialog.Builder(requireContext())
                .setTitle(member.fullName)
                .setMessage("Role: " + member.role + "\n" +
                        "Email: " + member.email + "\n" +
                        "Status: " + (member.isOnline ? "🟢 Online" : "⚪ Offline") + "\n" +
                        "Assigned Tasks: " + member.assignedTasksCount)
                .setPositiveButton("Close", null)
                .show();
    }

    private void displayGroupDetailsDialog(ChatGroupDto group) {
        new AlertDialog.Builder(requireContext())
                .setTitle(group.name)
                .setMessage("Topic: " + (group.description != null ? group.description : "Project squad discussion") + "\n" +
                        "Channel: #" + group.channel + "\n" +
                        "Squad Members: " + (group.members != null ? group.members.size() : 0) + " members")
                .setPositiveButton("Close", null)
                .show();
    }

    private void displayBasicDetailsDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = parser.parse(isoString);
            return d != null ? timeFormat.format(d) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
