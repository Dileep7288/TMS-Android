package com.example.myapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
public class EditTaskActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://172.16.20.76:8000/api/";
    private static final String DATE_FORMAT_API = "yyyy-MM-dd";
    private static final String DATE_FORMAT_DISPLAY = "MMM dd, yyyy";
    EditText titleEditText;
    EditText descriptionEditText;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private TextView dueDatePicker;
    private Button updateButton;
    private Date dueDate;
    private int taskId;
    private SimpleDateFormat apiDateFormat;
    private SimpleDateFormat displayDateFormat;
    private RequestQueue requestQueue;

    private static final String[] STATUS_OPTIONS = {
            "yet-to-start", "in-progress", "completed", "hold"
    };
    private static final String[] PRIORITY_OPTIONS = {
            "low", "medium", "high"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        initializeComponents();
        setupUI();
        loadTaskData();
    }

    private void initializeComponents() {
        requestQueue = Volley.newRequestQueue(this);
        apiDateFormat = new SimpleDateFormat(DATE_FORMAT_API, Locale.getDefault());
        displayDateFormat = new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.getDefault());

        titleEditText = findViewById(R.id.edit_task_title);
        descriptionEditText = findViewById(R.id.edit_task_description);
        statusSpinner = findViewById(R.id.edit_task_status);
        prioritySpinner = findViewById(R.id.edit_task_priority);
        dueDatePicker = findViewById(R.id.edit_task_due_date);
        updateButton = findViewById(R.id.update_task_button);
    }

    private void setupUI() {
        setupSpinners();
        setupDatePicker();
        setupUpdateButton();
    }

    private void setupSpinners() {
        setupSpinner(statusSpinner, STATUS_OPTIONS);
        setupSpinner(prioritySpinner, PRIORITY_OPTIONS);
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadTaskData() {
        Intent intent = getIntent();
        taskId = intent.getIntExtra("task_id", -1);
        titleEditText.setText(intent.getStringExtra("task_title"));
        descriptionEditText.setText(intent.getStringExtra("task_description"));

        setSpinnerSelection(statusSpinner, intent.getStringExtra("task_status"));
        setSpinnerSelection(prioritySpinner, intent.getStringExtra("task_priority"));

        String dueDateStr = intent.getStringExtra("task_due_date");
        if (dueDateStr != null) {
            try {
                dueDate = apiDateFormat.parse(dueDateStr);
                dueDatePicker.setText(displayDateFormat.format(dueDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupDatePicker() {
        dueDatePicker.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (dueDate != null) {
            if (dueDate.before(calendar.getTime())) {
                dueDatePicker.setText("Select Due Date");
                dueDate = null;
            } else {
                calendar.setTime(dueDate);
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    dueDate = selectedDate.getTime();
                    dueDatePicker.setText(displayDateFormat.format(dueDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void setupUpdateButton() {
        updateButton.setOnClickListener(v -> updateTask());
    }

    private void updateTask() {
        try {
            JSONObject taskData = createTaskData();
            sendUpdateRequest(taskData);
        } catch (Exception e) {
            handleError(e);
        }
    }

    private JSONObject createTaskData() throws Exception {
        JSONObject taskData = new JSONObject();
        taskData.put("title", titleEditText.getText().toString().trim());
        taskData.put("description", descriptionEditText.getText().toString().trim());
        taskData.put("status", statusSpinner.getSelectedItem().toString());
        taskData.put("priority", prioritySpinner.getSelectedItem().toString());
        if (dueDate != null) {
            taskData.put("deadline", apiDateFormat.format(dueDate));
        }
        return taskData;
    }

    private void sendUpdateRequest(JSONObject taskData) {
        String url = BASE_URL + "tasks/" + taskId + "/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                taskData,
                response -> handleSuccess(),
                this::handleApiError
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

    private void handleSuccess() {
        Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleApiError(VolleyError error) {
        String message = "Failed to update task";
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
                JSONObject data = new JSONObject(responseBody);
                message = data.optString("detail", message);
            } catch (Exception e) {
                message += " (Error " + error.networkResponse.statusCode + ")";
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private String getAccessToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}