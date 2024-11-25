package com.example.myapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class DashBoardActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener{
    private TextView totalTasksTextView;
    private TextView completedTasksTextView;
    private TextView pendingTasksTextView;
    private TextView priorityTaskCountTextView;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private TextView startDatePicker;
    private TextView endDatePicker;
    private Button applyFilters;
    private Button clearFilters;
    private Date startDate;
    private Date endDate;
    private List<Task> allTasks;
    private static final String DATE_FORMAT_DISPLAY = "MMM dd, yyyy";
    private static final String DATE_FORMAT_API = "yyyy-MM-dd";
    private SimpleDateFormat displayDateFormat;
    private SimpleDateFormat apiDateFormat;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;
    private ImageButton logoutButton;
    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://172.16.20.76:8000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        requestQueue = Volley.newRequestQueue(this);

        logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        displayDateFormat = new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.getDefault());
        apiDateFormat = new SimpleDateFormat(DATE_FORMAT_API, Locale.getDefault());

        initializeViews();
        initializeFilterViews();
        setupSpinners();
        setupDatePickers();
        setupFilterButtons();
        setupFab();
        fetchTasksFromServer();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void setupFab() {
        fabAddTask = findViewById(R.id.fab_add_task);
        fabAddTask.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateTaskActivity.class);
            startActivity(i);
        });
    }

    private void initializeViews() {
        totalTasksTextView = findViewById(R.id.total_task_count);
        completedTasksTextView = findViewById(R.id.completed_task_count);
        pendingTasksTextView = findViewById(R.id.pending_task_count);
        priorityTaskCountTextView = findViewById(R.id.priority_task_count);
        tasksRecyclerView = findViewById(R.id.tasks_recycler_view);
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Add item decoration for spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.task_item_spacing);
        tasksRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = spacing;
            }
        });
    }
    private void initializeFilterViews() {
        statusSpinner = findViewById(R.id.status_spinner);
        prioritySpinner = findViewById(R.id.priority_spinner);
        startDatePicker = findViewById(R.id.start_date_picker);
        endDatePicker = findViewById(R.id.end_date_picker);
        applyFilters = findViewById(R.id.apply_filters);
        clearFilters = findViewById(R.id.clear_filters);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"All", "yet-to-start", "in-progress", "completed", "hold"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        ArrayAdapter<CharSequence> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"All", "low", "medium", "high"});
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void setupDatePickers() {
        startDatePicker.setText("Select Start Date");
        endDatePicker.setText("Select End Date");
        startDatePicker.setOnClickListener(v -> showDatePicker(true));
        endDatePicker.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupFilterButtons() {
        applyFilters.setOnClickListener(v -> applyTaskFilters());
        clearFilters.setOnClickListener(v -> clearFilters());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartDate && endDate != null) {
            calendar.setTime(endDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    setToStartOfDay(selectedDate);

                    Date selectedDateTime = selectedDate.getTime();
                    TextView dateText = isStartDate ? startDatePicker : endDatePicker;

                    if (isStartDate) {
                        startDate = selectedDateTime;
                        String formattedDate = displayDateFormat.format(startDate);
                        dateText.setText(formattedDate);
                        Log.d("DatePicker", "Set start date: " + formattedDate);
                    } else {
                        endDate = selectedDateTime;
                        String formattedDate = displayDateFormat.format(endDate);
                        dateText.setText(formattedDate);
                        Log.d("DatePicker", "Set end date: " + formattedDate);
                    }

                    applyTaskFilters();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void setToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }
    private void fetchTasksFromServer() {
        String url = BASE_URL + "tasks/list/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray tasksArray = response.getJSONArray("results");
                        List<Task> tasks = new ArrayList<>();

                        for (int i = 0; i < tasksArray.length(); i++) {
                            JSONObject taskObject = tasksArray.getJSONObject(i);
                            Task task = parseTaskFromJson(taskObject);
                            if (task != null) {
                                tasks.add(task);
                            }
                        }

                        allTasks = new ArrayList<>(tasks);
                        updateUIWithTasks(tasks);

                    } catch (Exception e) {
                        Log.e("TaskFetch", "Error parsing tasks", e);
                        Toast.makeText(this, "Error parsing tasks", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> handleVolleyError(error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private Task parseTaskFromJson(JSONObject taskObject) {
        try {
            int id = taskObject.getInt("id");
            String status = taskObject.getString("status");
            String priority = taskObject.getString("priority");
            String title = taskObject.getString("title");
            String description = taskObject.getString("description");

            Date dueDate = null;
            if (!taskObject.isNull("deadline")) {
                String deadlineStr = taskObject.getString("deadline");
                try {
                    dueDate = apiDateFormat.parse(deadlineStr);
                } catch (ParseException e) {
                    Log.e("DateParse", "Error parsing deadline", e);
                }
            }

            return new Task(id, status, priority, dueDate, title, description);
        } catch (Exception e) {
            Log.e("TaskParse", "Error parsing task", e);
            return null;
        }
    }

    private void applyTaskFilters() {
        if (allTasks == null || allTasks.isEmpty()) {
            Toast.makeText(this, "No tasks to filter", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String selectedStatus = statusSpinner.getSelectedItem().toString();
            String selectedPriority = prioritySpinner.getSelectedItem().toString();

            // Log all tasks before filtering
            Log.d("TaskFilter", "Before filtering - Total tasks: " + allTasks.size());
            for (Task task : allTasks) {
                Log.d("TaskFilter", String.format("Task - Title: %s, Status: %s, Priority: %s, Due Date: %s",
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDueDate() != null ? apiDateFormat.format(task.getDueDate()) : "null"));
            }

            TaskFilter filter = new TaskFilter.Builder()
                    .setStatus(selectedStatus)
                    .setPriority(selectedPriority)
                    .setDateRange(startDate, endDate)
                    .build();

            List<Task> filteredTasks = filter.apply(allTasks);

            // Log filter parameters
            Log.d("TaskFilter", String.format("Filter parameters - Status: %s, Priority: %s",
                    selectedStatus, selectedPriority));
            if (startDate != null || endDate != null) {
                Log.d("TaskFilter", String.format("Date range: %s to %s",
                        startDate != null ? apiDateFormat.format(startDate) : "none",
                        endDate != null ? apiDateFormat.format(endDate) : "none"));
            }

            // Log filtered results
            Log.d("TaskFilter", "After filtering - Tasks: " + filteredTasks.size());
            for (Task task : filteredTasks) {
                Log.d("TaskFilter", String.format("Filtered Task - Title: %s, Status: %s, Priority: %s, Due Date: %s",
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getDueDate() != null ? apiDateFormat.format(task.getDueDate()) : "null"));
            }

            updateUIWithTasks(filteredTasks);

        } catch (IllegalArgumentException e) {
            Log.e("TaskFilter", "Filter error: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> performDelete(task))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(Task task) {
        String url = BASE_URL + "tasks/delete/" + task.getId() + "/";
        Log.d("DeleteTask", "Attempting to delete task at URL: " + url);

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    allTasks.remove(task);
                    updateUIWithTasks(allTasks);
                    Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String message = "Failed to delete task";
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            if (!responseBody.isEmpty()) {
                                JSONObject data = new JSONObject(responseBody);
                                message = data.optString("detail", message);
                            }
                        } catch (Exception e) {
                            Log.e("DeleteTask", "Error parsing error response", e);
                        }
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    Log.e("DeleteTask", "Error: " + error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTasksFromServer();
    }
    private void performLogout() {
        String url = BASE_URL + "logout/";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    clearUserSession();
                    navigateToMain();
                },
                this::handleVolleyError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void clearUserSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("access_token");
        editor.remove("refresh_token");
        editor.apply();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleVolleyError(VolleyError error) {
        String message = "An error occurred";
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
                if (!responseBody.isEmpty()) {
                    JSONObject data = new JSONObject(responseBody);
                    message = data.optString("detail", message);
                } else {
                    message = "Error " + error.networkResponse.statusCode;
                }
            } catch (Exception e) {
                message = "Error " + error.networkResponse.statusCode;
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("VolleyError", "Error: " + message, error);
    }
    @Override
    public void onEditTask(Task task) {
        Intent i = new Intent(this, EditTaskActivity.class);
        i.putExtra("task_id", task.getId());
        i.putExtra("task_title", task.getTitle());
        i.putExtra("task_description", task.getDescription());
        i.putExtra("task_status", task.getStatus());
        i.putExtra("task_priority", task.getPriority());
        if (task.getDueDate() != null) {
            i.putExtra("task_due_date", apiDateFormat.format(task.getDueDate()));
        }
        startActivity(i);
    }
    private void clearFilters() {
        statusSpinner.setSelection(0);
        prioritySpinner.setSelection(0);
        startDate = null;
        endDate = null;
        startDatePicker.setText("Select Start Date");
        endDatePicker.setText("Select End Date");

        if (allTasks != null) {
            updateUIWithTasks(allTasks);
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIWithTasks(List<Task> tasks) {
        int totalTasks = tasks.size();
        int completedTasks = 0;
        int pendingTasks = 0;
        int lowPriorityCount = 0;
        int mediumPriorityCount = 0;
        int highPriorityCount = 0;

        for (Task task : tasks) {
            switch (task.getStatus().toLowerCase()) {
                case "completed":
                    completedTasks++;
                    break;
                case "yet-to-start":
                case "in-progress":
                    pendingTasks++;
                    break;
            }

            switch (task.getPriority().toLowerCase()) {
                case "low":
                    lowPriorityCount++;
                    break;
                case "medium":
                    mediumPriorityCount++;
                    break;
                case "high":
                    highPriorityCount++;
                    break;
            }
        }

        totalTasksTextView.setText(String.valueOf(totalTasks));
        completedTasksTextView.setText(String.valueOf(completedTasks));
        pendingTasksTextView.setText(String.valueOf(pendingTasks));
        priorityTaskCountTextView.setText(String.format("Low: %d, Medium: %d, High: %d",
                lowPriorityCount, mediumPriorityCount, highPriorityCount));

        taskAdapter.setTasks(tasks);
    }
}