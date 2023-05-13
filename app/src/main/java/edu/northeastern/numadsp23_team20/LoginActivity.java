package edu.northeastern.numadsp23_team20;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginBtn;
    private TextView signUpText, forgotPassText;
    FirebaseAuth mAuth;
    String loginEmail, loginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.loginEmailEditText);
        passwordEditText = findViewById(R.id.loginPasswordEditText);
        forgotPassText = findViewById(R.id.forgotPassText);
        signUpText = findViewById(R.id.signUpText);
        loginBtn = findViewById(R.id.loginButton);

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> {
            if (!validateEmail() | !validatePassword()) {
                return;
            }
            mAuth.signInWithEmailAndPassword(loginEmail, loginPassword).
                    addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            SharedPreferences.Editor editor = this.getSharedPreferences(GeoNotif.PREFERENCES, MODE_PRIVATE).edit();
                            editor.putString(GeoNotif.NOTIF_SETTING, GeoNotif.ENABLE_NOTIF_SETTING);
                            editor.apply();
                            Intent intent = new Intent(LoginActivity.this, HomePage.class);
                            startActivity(intent);
                            finish();
                        } else {
                            System.out.println(task.getException().getLocalizedMessage());
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        forgotPassText.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            View dialogView = getLayoutInflater().inflate(R.layout.activity_forgot_password, null);
            EditText emailBox = dialogView.findViewById(R.id.resetEmailEditText);
            builder.setView(dialogView);
            AlertDialog dialog = builder.create();
            dialogView.findViewById(R.id.resetButton).setOnClickListener(view12 -> {
                String userEmail = emailBox.getText().toString();
                if (TextUtils.isEmpty(userEmail) && !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                    emailBox.setError("Invalid Email ID!");
                    return;
                }
                mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Check your email", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(LoginActivity.this, "Unable to send, failed", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            dialogView.findViewById(R.id.cancelButton).setOnClickListener(view1 -> dialog.dismiss());
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            dialog.show();
        });
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateEmail() {
        loginEmail = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(loginEmail)) {
            emailEditText.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(loginEmail).matches()) {
            emailEditText.setError("Enter a valid email");
            return false;
        } else {
            return true;
        }
    }

    private boolean validatePassword() {
        loginPassword = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(loginPassword)) {
            emailEditText.setError("Password is required");
            return false;
        } else {
            return true;
        }
    }
}