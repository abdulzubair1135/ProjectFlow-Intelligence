package com.projectflow.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.projectflow.android.data.remote.model.DtoModels.NotificationDto;
import com.projectflow.android.data.repository.ProjectFlowRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private TextView chipAllNotifs, chipUnreadNotifs, btnMarkAllRead;
    private RecyclerView rvNotifications;
    private LinearLayout llEmptyNotifs;
    private SwipeRefreshLayout swipeRefreshNotifs;
    private ProjectFlowRepository repository;

    private NotificationsAdapter adapter;
    private final List<NotificationDto> allList = new ArrayList<>();
    private boolean showOnlyUnread = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProjectFlowRepository(requireContext());

        chipAllNotifs = view.findViewById(R.id.chipAllNotifs);
        chipUnreadNotifs = view.findViewById(R.id.chipUnreadNotifs);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        llEmptyNotifs = view.findViewById(R.id.llEmptyNotifs);
        swipeRefreshNotifs = view.findViewById(R.id.swipeRefreshNotifs);

        adapter = new NotificationsAdapter(notif -> {
            if (!notif.read) {
                notif.read = true;
                adapter.notifyDataSetChanged();
                repository.markNotificationRead(notif.id, null);
            }
            if (notif.taskId != null && !notif.taskId.isEmpty()) {
                android.content.Intent intent = new android.content.Intent(requireContext(), com.projectflow.android.ui.TaskDetailsActivity.class);
                intent.putExtra("task_id", notif.taskId);
                intent.putExtra("task_title", notif.title);
                intent.putExtra("task_desc", notif.message);
                startActivity(intent);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        chipAllNotifs.setOnClickListener(v -> {
            showOnlyUnread = false;
            chipAllNotifs.setBackgroundResource(R.drawable.bg_chip_selected);
            chipAllNotifs.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            chipUnreadNotifs.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipUnreadNotifs.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            filterList();
        });

        chipUnreadNotifs.setOnClickListener(v -> {
            showOnlyUnread = true;
            chipUnreadNotifs.setBackgroundResource(R.drawable.bg_chip_selected);
            chipUnreadNotifs.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            chipAllNotifs.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipAllNotifs.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            filterList();
        });

        btnMarkAllRead.setOnClickListener(v -> {
            for (NotificationDto n : allList) n.read = true;
            adapter.notifyDataSetChanged();
            repository.markAllNotificationsRead(null);
        });

        swipeRefreshNotifs.setColorSchemeResources(R.color.primary_earth, R.color.muted_clay);
        swipeRefreshNotifs.setOnRefreshListener(this::loadNotifications);

        loadNotifications();
    }

    private void loadNotifications() {
        repository.fetchNotifications(new ProjectFlowRepository.DataCallback<List<NotificationDto>>() {
            @Override
            public void onData(List<NotificationDto> data) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshNotifs.setRefreshing(false);
                        allList.clear();
                        if (data != null) allList.addAll(data);
                        filterList();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> swipeRefreshNotifs.setRefreshing(false));
                }
            }
        });
    }

    private void filterList() {
        List<NotificationDto> filtered = new ArrayList<>();
        for (NotificationDto n : allList) {
            if (!showOnlyUnread || !n.read) {
                filtered.add(n);
            }
        }
        adapter.setList(filtered);

        if (filtered.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            llEmptyNotifs.setVisibility(View.VISIBLE);
        } else {
            llEmptyNotifs.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private static class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
        private final List<NotificationDto> list = new ArrayList<>();
        private final OnNotifClickListener listener;

        interface OnNotifClickListener { void onClick(NotificationDto notif); }

        NotificationsAdapter(OnNotifClickListener listener) { this.listener = listener; }

        void setList(List<NotificationDto> items) {
            list.clear();
            if (items != null) list.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationDto item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvSubtitle.setText(item.message);
            holder.tvTime.setText(formatRelativeTime(item.createdAt));
            holder.unreadDot.setVisibility(item.read ? View.GONE : View.VISIBLE);

            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        private String formatRelativeTime(String isoTime) {
            if (isoTime == null) return "Recently";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date d = sdf.parse(isoTime.split("\\.")[0]);
                if (d == null) return "Recently";
                long diffMs = System.currentTimeMillis() - d.getTime();
                long mins = diffMs / 60000;
                if (mins < 1) return "Just now";
                if (mins < 60) return mins + "m ago";
                long hours = mins / 60;
                if (hours < 24) return hours + "h ago";
                long days = hours / 24;
                return days + "d ago";
            } catch (Exception e) {
                return "Today";
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvTime;
            View unreadDot;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvSubtitle = itemView.findViewById(R.id.tvNotifSubtitle);
                tvTime = itemView.findViewById(R.id.tvNotifTime);
                unreadDot = itemView.findViewById(R.id.unreadDot);
            }
        }
    }
}
