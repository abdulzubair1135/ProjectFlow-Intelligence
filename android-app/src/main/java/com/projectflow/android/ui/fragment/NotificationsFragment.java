package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.projectflow.android.R;

public class NotificationsFragment extends Fragment {

    private static final String[][] NOTIF_DATA = {
            {"Task assigned to you", "Design Login Screen", "2 minutes ago"},
            {"Project updated", "Client Dashboard", "15 minutes ago"},
            {"New comment", "Website Redesign", "1 hour ago"},
            {"Project deadline approaching", "Marketing Campaign", "3 hours ago"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);
        ViewGroup containerLayout = v.findViewById(R.id.notificationsContainer);
        containerLayout.removeAllViews();

        for (String[] notif : NOTIF_DATA) {
            View itemView = inflater.inflate(R.layout.item_notification, containerLayout, false);
            TextView tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            TextView tvSub = itemView.findViewById(R.id.tvNotifSubtitle);
            TextView tvTime = itemView.findViewById(R.id.tvNotifTime);

            tvTitle.setText(notif[0]);
            tvSub.setText(notif[1]);
            tvTime.setText(notif[2]);

            containerLayout.addView(itemView);
        }

        return v;
    }
}
