package com.example.myapp;

// Android core imports
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

// AndroidX imports
import androidx.appcompat.app.AppCompatActivity;

// JSON imports
import org.json.JSONObject;
import org.json.JSONException;

// Volley imports
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class AdminLoginPage extends AppCompatActivity {

    private EditText adminuserName,adminPassword;
    private String adminusername,adminpassword;
    private final String Admin_Login_Url="http://172.16.20.76:8000/api/superuser/login/";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle sv){
        super.onCreate(sv);
        setContentView(R.layout.activity_admin_login);

        sharedPreferences=getSharedPreferences("AdminPrefs",MODE_PRIVATE);
        adminuserName=findViewById(R.id.username_input);
        adminPassword=findViewById(R.id.password_input);

        findViewById(R.id.login_btn).setOnClickListener(v->adminlogin());
    }
    public void adminlogin(){
        adminusername=adminuserName.getText().toString().trim();
        adminpassword=adminPassword.getText().toString().trim();

        if(adminusername.isEmpty()||adminpassword.isEmpty()){
            Toast.makeText(this,"Please enter email and password fields", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject obj=new JSONObject();
            obj.put("username",adminusername);
            obj.put("password",adminpassword);

            JsonObjectRequest req=new JsonObjectRequest(
                    Request.Method.POST,
                    Admin_Login_Url,
                    obj,
                    response -> {
                        try {
                                if(response.has("status") && response.getString("status").equals("success")){
                                    JSONObject data=response.getJSONObject("data");
                                    JSONObject tokens=data.getJSONObject("tokens");

                                    String accessToken = tokens.getString("access");
                                    String refreshToken = tokens.getString("refresh");

                                    saveTokens(accessToken,refreshToken);
                                    navigateToAdminDashboard(data);
                                    Toast.makeText(AdminLoginPage.this,"Login Successfull",Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(this,"Login Failed",Toast.LENGTH_SHORT).show();
                                }
                } catch (JSONException e) {
                    Toast.makeText(this,"Error Parsing response"+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }, error -> {
                Toast.makeText(this,"Network error"+error.getMessage(),Toast.LENGTH_SHORT).show();
            });
            RequestQueue rq=Volley.newRequestQueue(this);
            rq.add(req);
        }
        catch (JSONException e) {
            Toast.makeText(this,"Error Creating Request"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTokens(String accessToken,String refreshToken){
        SharedPreferences.Editor ed= sharedPreferences.edit();
        ed.putString("admin_access_token",accessToken);
        ed.putString("admin_refresh_token",refreshToken);
        ed.apply();
    }
    private void navigateToAdminDashboard(JSONObject data){
        try {
            JSONObject user = data.getJSONObject("user");
            String username = user.getString("username");
            String email = user.getString("email");
            Intent i = new Intent(AdminLoginPage.this, AdminDashBoard.class);
            i.putExtra("admin_username",username);
            i.putExtra("admin_email",email);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,"Did not get the user data",Toast.LENGTH_SHORT).show();
        }
    }
}
