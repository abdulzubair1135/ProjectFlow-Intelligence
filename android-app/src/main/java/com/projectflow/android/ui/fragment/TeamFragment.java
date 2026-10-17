package com.projectflow.android.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.projectflow.android.R;
import com.projectflow.android.data.remote.model.DtoModels.TeamMemberDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;
import com.projectflow.android.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class TeamFragment extends Fragment {

    private EditText etSearchTeam;
    private TextView chipRoleAll, chipRoleAdmin, chipRoleManager, chipRoleMember;
    private RecyclerView rvTeamMembers;
    private LinearLayout llEmptyTeam;
    private SwipeRefreshLayout swipeRefreshTeam;
    private ProjectFlowRepository repository;

    private TeamMemberAdapter adapter;
    private final List<TeamMemberDto> fullTeamList = new ArrayList<>();
    private String selectedRole = "ALL";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_team, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());

        etSearchTeam = view.findViewById(R.id.etSearchTeam);
        chipRoleAll = view.findViewById(R.id.chipRoleAll);
        chipRoleAdmin = view.findViewById(R.id.chipRoleAdmin);
        chipRoleManager = view.findViewById(R.id.chipRoleManager);
        chipRoleMember = view.findViewById(R.id.chipRoleMember);
        rvTeamMembers = view.findViewById(R.id.rvTeamMembers);
        llEmptyTeam = view.findViewById(R.id.llEmptyTeam);
        swipeRefreshTeam = view.findViewById(R.id.swipeRefreshTeam);

        adapter = new TeamMemberAdapter(member -> showMemberDetailsDialog(member));
        rvTeamMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTeamMembers.setAdapter(adapter);

        etSearchTeam.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = (s != null) ? s.toString().trim().toLowerCase() : "";
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipRoleAll.setOnClickListener(v -> selectRoleFilter("ALL", chipRoleAll));
        chipRoleAdmin.setOnClickListener(v -> selectRoleFilter("ADMINISTRATOR", chipRoleAdmin));
        chipRoleManager.setOnClickListener(v -> selectRoleFilter("MANAGER", chipRoleManager));
        chipRoleMember.setOnClickListener(v -> selectRoleFilter("MEMBER", chipRoleMember));

        swipeRefreshTeam.setColorSchemeResources(R.color.primary_earth, R.color.muted_clay);
        swipeRefreshTeam.setOnRefreshListener(this::loadTeamData);

        loadTeamData();
    }

    private void selectRoleFilter(String role, TextView selectedChip) {
        selectedRole = role;
        TextView[] chips = {chipRoleAll, chipRoleAdmin, chipRoleManager, chipRoleMember};
        for (TextView c : chips) {
            if (c == selectedChip) {
                c.setBackgroundResource(R.drawable.bg_chip_selected);
                c.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                c.setBackgroundResource(R.drawable.bg_chip_unselected);
                c.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            }
        }
        applyFilter();
    }

    private void loadTeamData() {
        repository.fetchTeam(new ProjectFlowRepository.DataCallback<List<TeamMemberDto>>() {
            @Override
            public void onData(List<TeamMemberDto> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshTeam.setRefreshing(false);
                        fullTeamList.clear();
                        if (data != null) fullTeamList.addAll(data);
                        applyFilter();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> swipeRefreshTeam.setRefreshing(false));
                }
            }
        });
    }

    private void applyFilter() {
        List<TeamMemberDto> filtered = new ArrayList<>();
        for (TeamMemberDto m : fullTeamList) {
            boolean matchesRole = "ALL".equals(selectedRole) || selectedRole.equalsIgnoreCase(m.role);
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (m.fullName != null && m.fullName.toLowerCase().contains(searchQuery)) ||
                    (m.email != null && m.email.toLowerCase().contains(searchQuery)) ||
                    (m.role != null && m.role.toLowerCase().contains(searchQuery));

            if (matchesRole && matchesSearch) {
                filtered.add(m);
            }
        }

        adapter.setMembers(filtered);

        if (filtered.isEmpty()) {
            rvTeamMembers.setVisibility(View.GONE);
            llEmptyTeam.setVisibility(View.VISIBLE);
        } else {
            llEmptyTeam.setVisibility(View.GONE);
            rvTeamMembers.setVisibility(View.VISIBLE);
        }
    }

        private void showMemberDetailsDialog(TeamMemberDto member) {
        new AlertDialog.Builder(requireContext())
                .setTitle(member.fullName)
                .setMessage("Role: " + member.role + "\n" +
                        "Email: " + member.email + "\n" +
                        "Status: " + (member.active ? "Active Member" : "Inactive") + "\n" +
                        "Assigned Tasks: " + member.assignedTasksCount)
                .setPositiveButton("Send Message", (d, w) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToFragment(ChatFragment.newInstance("general"), "Team Discussions");
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private static class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.ViewHolder> {
        private final List<TeamMemberDto> members = new ArrayList<>();
        private final OnMemberClickListener listener;

        interface OnMemberClickListener { void onClick(TeamMemberDto member); }

        TeamMemberAdapter(OnMemberClickListener listener) { this.listener = listener; }

        void setMembers(List<TeamMemberDto> items) {
            members.clear();
            if (items != null) members.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_member, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TeamMemberDto m = members.get(position);
            holder.tvName.setText(m.fullName);
            holder.tvRole.setText(m.role);
            holder.tvProjects.setText(m.assignedTasksCount + (m.assignedTasksCount == 1 ? " Task Assigned" : " Tasks Assigned"));

            String initial = (m.fullName != null && !m.fullName.isEmpty()) ? m.fullName.substring(0, 1).toUpperCase() : "U";
            holder.tvInitial.setText(initial);

            holder.itemView.setOnClickListener(v -> listener.onClick(m));
        }

        @Override
        public int getItemCount() { return members.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRole, tvProjects, tvInitial;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvMemberName);
                tvRole = itemView.findViewById(R.id.tvMemberRole);
                tvProjects = itemView.findViewById(R.id.tvMemberProjects);
                tvInitial = itemView.findViewById(R.id.tvAvatarInitial);
            }
        }
    }
}
