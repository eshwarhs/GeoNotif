package edu.northeastern.numadsp23_team20;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SignupActivity extends AppCompatActivity {
    private EditText fullNameEditText, usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    String fullName, username, email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        fullNameEditText = findViewById(R.id.fullnameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginTextView = findViewById(R.id.loginTextView);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("GeoNotif");

        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            if (!validateFullname() | !validateUsername() | !validateEmail() | !validatePassword()) {
                return;
            }
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener
                    (task -> {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            User data = new User(fullName, username, email, uid);
                            FirebaseDatabase.getInstance().getReference("GeoNotif/Users")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(data).
                                    addOnCompleteListener(task1 -> {
                                        SharedPreferences.Editor editor = this.getSharedPreferences(GeoNotif.PREFERENCES, MODE_PRIVATE).edit();
                                        editor.putString(GeoNotif.NOTIF_SETTING, GeoNotif.ENABLE_NOTIF_SETTING);
                                        editor.apply();
                                        Intent intent = new Intent(SignupActivity.this, HomePage.class);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            System.out.println(task.getException().getLocalizedMessage());
                            if (task.getException().getLocalizedMessage().equals("The email address is already in use by another account.")) {
                                emailEditText.setError("Email is already in use.");
                            } else {
                                Toast.makeText(SignupActivity.this, "Check Email or Password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }

    private boolean validateFullname() {
        fullName = fullNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("FullName is required");
            return false;
        } else {
            return true;
        }
    }

    private boolean validateUsername() {
        username = usernameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            return false;
        } else {
            return true;
        }
    }

    private boolean validateEmail() {
        email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return false;
        } else {
            return true;
        }
    }

    private boolean validatePassword() {
        password = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return false;
        } else if (password.length() <= 6) {
            passwordEditText.setError("Password is Very Short");
            return false;
        } else {
            return true;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}