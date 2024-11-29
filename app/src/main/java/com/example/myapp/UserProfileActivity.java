package com.example.myapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {
    private CircleImageView profileImage;
    private TextInputEditText usernameEdit, emailEdit, newPasswordEdit;
    private MaterialButton saveButton;
    private ProgressBar progressBar;
    private Uri selectedImageUri;
    private ImageButton editPhotoButton;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String BASE_URL = "http://172.16.20.76:8000/";
    private static final String PROFILE_URL = BASE_URL + "api/user/profile/";
    private static final String UPDATE_PROFILE_URL = BASE_URL + "api/user/update-profile/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initViews();
        loadUserProfile();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        usernameEdit = findViewById(R.id.username_edit);
        emailEdit = findViewById(R.id.email_edit);
        newPasswordEdit = findViewById(R.id.new_password_edit);
        saveButton = findViewById(R.id.save_button);
        progressBar = findViewById(R.id.progress_bar);
        editPhotoButton = findViewById(R.id.edit_photo_button);

        editPhotoButton.setOnClickListener(v -> openImagePicker());
        profileImage.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveChanges());

        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Profile");
        }
    }

    private void loadUserProfile() {
        showProgress(true);
        String token = getStoredToken();

        if (token.isEmpty()) {
            showError("No authentication token found");
            navigateToLogin();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.GET,
                PROFILE_URL,
                response -> handleProfileResponse(response),
                error -> handleError(error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }

    private void handleProfileResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String username = jsonResponse.optString("username", "");
            String email = jsonResponse.optString("email", "");
            String photoUrl = jsonResponse.optString("photo", "");

            runOnUiThread(() -> {
                usernameEdit.setText(username);
                emailEdit.setText(email);
                loadProfileImage(photoUrl);
            });
        } catch (JSONException e) {
            showError("Failed to parse profile data");
        } finally {
            showProgress(false);
        }
    }

    private void loadProfileImage(String photoUrl) {
        String fullPhotoUrl = photoUrl;
        if (photoUrl != null && !photoUrl.isEmpty() && !photoUrl.startsWith("http")) {
            fullPhotoUrl = BASE_URL + photoUrl.replaceFirst("^/+", "");
        }

        Glide.with(this)
                .load(fullPhotoUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(profileImage);
    }

    private void saveChanges() {
        if (!validateInputs()) return;
        showProgress(true);

        MultipartRequest multipartRequest = createMultipartRequest();
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private MultipartRequest createMultipartRequest() {
        return new MultipartRequest(
                Request.Method.PUT,
                UPDATE_PROFILE_URL,
                this::handleUpdateResponse,
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + getStoredToken());
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", usernameEdit.getText().toString().trim());
                params.put("email", emailEdit.getText().toString().trim());

                String newPassword = newPasswordEdit.getText().toString();
                if (!newPassword.isEmpty()) {
                    params.put("password", newPassword);
                }
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                if (selectedImageUri != null) {
                    try {
                        byte[] imageData = getImageData(selectedImageUri);
                        params.put("photo", new DataPart("profile.jpg", imageData, "image/jpeg"));
                    } catch (IOException e) {
                        showError("Failed to process image");
                    }
                }
                return params;
            }
        };
    }

    private void handleUpdateResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            JSONObject jsonResponse = new JSONObject(jsonString);
            String message = jsonResponse.optString("message", "Profile updated successfully");

            runOnUiThread(() -> {
                showError(message);
                loadUserProfile();
            });
        } catch (Exception e) {
            showError("Failed to update profile");
        } finally {
            showProgress(false);
        }
    }

    private void handleError(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
            clearUserSession();
            navigateToLogin();
            return;
        }
        showError("Network error occurred");
        showProgress(false);
    }

    private byte[] getImageData(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) throw new IOException("Failed to open image");

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) throw new IOException("Failed to decode image");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            return outputStream.toByteArray();
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .centerCrop()
                        .into(profileImage);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    private boolean validateInputs() {
        String username = usernameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEdit.setError("Username is required");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.setError("Valid email is required");
            return false;
        }
        return true;
    }

    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!show);
            editPhotoButton.setEnabled(!show);
            profileImage.setEnabled(!show);
        });
    }

    private void clearUserSession() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getStoredToken() {
        return getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getString("access_token", "");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}