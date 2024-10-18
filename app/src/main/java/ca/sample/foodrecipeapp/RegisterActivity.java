package ca.sample.foodrecipeapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText mName, mPhone, mEmail, mPassword;
    ProgressBar mProgressBar;
    FirebaseAuth firebaseAuth;
    String TAG = "TAG";
    FirebaseFirestore firebaseFirestore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Hide the soft keyboard initially
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Initializing views and Firebase instances
        mName = findViewById(R.id.txt_name);
        mPhone = findViewById(R.id.txt_phone);
        mEmail = findViewById(R.id.txt_email);
        mPassword = findViewById(R.id.txt_password);
        mProgressBar = findViewById(R.id.registerProgressBar);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    // Register user when the Register button is clicked
    public void register(final View view) {
        final String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        final String fullName = mName.getText().toString().trim();
        final String phone = mPhone.getText().toString().trim();

        // Input validation checks
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email is Required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            mPassword.setError("Password is Required.");
            return;
        }

        if (password.length() < 6) {
            mPassword.setError("Password Must be >= 6 Characters");
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);  // Show progress bar while registering

        // Registering user with Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Fetching user ID and saving user information to Firestore
                    userID = firebaseAuth.getCurrentUser().getUid();
                    DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
                    final Map<String, Object> mapUser = new HashMap<>();
                    mapUser.put("fullName", fullName);
                    mapUser.put("email", email);
                    mapUser.put("phone", phone);

                    // Saving user data to Firestore
                    documentReference.set(mapUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: User profile created for " + mapUser);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "onFailure: " + e.toString());
                        }
                    });

                    // Show success message and redirect to LoginActivity
                    Snackbar.make(view, "User Created", BaseTransientBottomBar.LENGTH_LONG).show();
                    Log.d(TAG, "Redirecting to LoginActivity after registration.");

                    // Sign out the user after registration to prevent auto-login
                    firebaseAuth.signOut();

                    // Close RegisterActivity and start LoginActivity
                    finish();  // Close RegisterActivity
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));  // Start LoginActivity
                } else {
                    // Show error message if registration fails
                    Snackbar.make(view, "Error ! " + task.getException().getMessage(), BaseTransientBottomBar.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.INVISIBLE);  // Hide progress bar
                }
                mProgressBar.setVisibility(View.INVISIBLE);  // Hide progress bar after completion
            }
        });
    }
}