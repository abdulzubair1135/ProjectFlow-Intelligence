package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;

public class TeamFragment extends Fragment {

    private static final String[][] TEAM_DATA = {
            {"Rahul Verma", "Project Manager", "Online"},
            {"Priya Sharma", "UI/UX Designer", "Online"},
            {"Amit Kumar", "Android Developer", "Offline"},
            {"Sneha Patel", "Backend Developer", "Online"},
            {"Vikram Singh", "Tester", "Online"},
            {"Neha Gupta", "Content Writer", "Offline"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_team, container, false);
        ViewGroup containerLayout = v.findViewById(R.id.teamContainer);
        containerLayout.removeAllViews();

        for (String[] member : TEAM_DATA) {
            View itemView = inflater.inflate(R.layout.item_team_member, containerLayout, false);
            TextView tvName = itemView.findViewById(R.id.tvMemberName);
            TextView tvRole = itemView.findViewById(R.id.tvMemberRole);
            TextView tvStatus = itemView.findViewById(R.id.tvMemberStatus);

            tvName.setText(member[0]);
            tvRole.setText(member[1]);
            tvStatus.setText(member[2]);

            if ("Offline".equalsIgnoreCase(member[2])) {
                tvStatus.setBackgroundColor(requireContext().getColor(R.color.mineral_grey));
                tvStatus.setTextColor(requireContext().getColor(R.color.charcoal));
            }

            containerLayout.addView(itemView);
        }

        ImageView btnAdd = v.findViewById(R.id.btnAddMember);
        btnAdd.setOnClickListener(view -> Toast.makeText(requireContext(), "Invite Team Member", Toast.LENGTH_SHORT).show());

        return v;
    }
}
