package com.example.myapp;

import android.app.DatePickerDialog;
import android.content.Context;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;
public class CreateTaskActivity extends AppCompatActivity {
    EditText titleEditText;
    EditText descriptionEditText;
    private Spinner statusSpinner;
    private Spinner prioritySpinner;
    private TextView dueDatePicker;
    private Button createButton;
    private Date dueDate;
    private SimpleDateFormat apiDateFormat;
    private SimpleDateFormat displayDateFormat;
    private RequestQueue requestQueue;
    private static final String CREATE_TASK_URL = "http://172.16.20.76:8000/api/tasks/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);
        setTitle("Create New Task");

        requestQueue = Volley.newRequestQueue(this);
        apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        initializeViews();
        setupSpinners();
        setupDatePicker();
        setupCreateButton();
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.create_task_title);
        descriptionEditText = findViewById(R.id.create_task_description);
        statusSpinner = findViewById(R.id.create_task_status);
        prioritySpinner = findViewById(R.id.create_task_priority);
        dueDatePicker = findViewById(R.id.create_task_due_date);
        createButton = findViewById(R.id.create_task_button);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"yet-to-start", "in-progress", "completed", "hold"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        ArrayAdapter<CharSequence> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"low", "medium", "high"});
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void setupDatePicker() {
        dueDatePicker.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (dueDate != null) {
                calendar.setTime(dueDate);
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

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

            datePickerDialog.show();
        });
    }

    private void setupCreateButton() {
        createButton.setOnClickListener(v -> {
            if (validateInput()) {
                createTask();
            }
        });
    }

    private boolean validateInput() {
        String title = titleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return false;
        }

        String description = descriptionEditText.getText().toString().trim();
        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return false;
        }

        return true;
    }

    private void createTask() {
        try {
            JSONObject taskData = new JSONObject();
            taskData.put("title", titleEditText.getText().toString().trim());
            taskData.put("description", descriptionEditText.getText().toString().trim());
            taskData.put("status", statusSpinner.getSelectedItem().toString());
            taskData.put("priority", prioritySpinner.getSelectedItem().toString());
            if (dueDate != null) {
                taskData.put("deadline", apiDateFormat.format(dueDate));
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    CREATE_TASK_URL,
                    taskData,
                    response -> {
                        Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        String message = "Failed to create task";
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
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + getAccessToken());
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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