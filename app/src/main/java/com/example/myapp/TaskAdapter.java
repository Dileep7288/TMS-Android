package com.example.myapp;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.ImageButton;
import android.widget.PopupMenu;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks;

    private final OnTaskActionListener listener;
    private SimpleDateFormat dateFormat;
    public interface OnTaskActionListener {
        void onDeleteTask(Task task);
        void onEditTask(Task task);
    }

    public TaskAdapter(OnTaskActionListener listener) {
        this.tasks = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        this.listener = listener;  // Initialize listener
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView statusText;
        private final TextView priorityText;
        private final TextView dueDateText;
        private final ImageButton menuButton;


        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.task_title);
            descriptionText = itemView.findViewById(R.id.task_description);
            statusText = itemView.findViewById(R.id.task_status);
            priorityText = itemView.findViewById(R.id.task_priority);
            dueDateText = itemView.findViewById(R.id.task_due_date);
            menuButton = itemView.findViewById(R.id.task_menu); // Add this line
        }


        public void bind(Task task) {
            titleText.setText(task.getTitle());
            descriptionText.setText(task.getDescription());
            statusText.setText(task.getStatus());
            priorityText.setText(task.getPriority());

            // Set status background color
            int statusColor;
            switch (task.getStatus().toLowerCase()) {
                case "completed":
                    statusColor = Color.parseColor("#4CAF50");
                    break;
                case "in-progress":
                    statusColor = Color.parseColor("#2196F3");
                    break;
                default:
                    statusColor = Color.parseColor("#FFA000");
                    break;
            }
            statusText.getBackground().setColorFilter(statusColor, PorterDuff.Mode.SRC_IN);

            // Set priority background color
            int priorityColor;
            switch (task.getPriority().toLowerCase()) {
                case "high":
                    priorityColor = Color.parseColor("#F44336");
                    break;
                case "medium":
                    priorityColor = Color.parseColor("#FF9800");
                    break;
                default:
                    priorityColor = Color.parseColor("#4CAF50");
                    break;
            }
            priorityText.getBackground().setColorFilter(priorityColor, PorterDuff.Mode.SRC_IN);

            // Set due date
            if (task.getDueDate() != null) {
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        .format(task.getDueDate());
                dueDateText.setText("Due: " + formattedDate);
                dueDateText.setVisibility(View.VISIBLE);
            } else {
                dueDateText.setVisibility(View.GONE);
            }

            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.inflate(R.menu.task_item_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_delete) {
                        listener.onDeleteTask(task);
                        return true;
                    } else if (itemId == R.id.action_edit) {
                        listener.onEditTask(task);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}