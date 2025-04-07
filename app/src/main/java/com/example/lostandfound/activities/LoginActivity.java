package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lostandfound.MainActivity;
import com.example.lostandfound.R;
import com.example.lostandfound.models.User;
import com.example.lostandfound.utils.FirebaseUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private SignInButton btnGoogleSignIn;
    private TextView tvSignup, tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        
        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        tvSignup = findViewById(R.id.tv_signup);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Set Google Sign In button text
        btnGoogleSignIn.setSize(SignInButton.SIZE_STANDARD);
        btnGoogleSignIn.setColorScheme(SignInButton.COLOR_LIGHT);

        // Check if coming from logout (FLAG_ACTIVITY_CLEAR_TASK would be set)
        boolean isFromLogout = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0;

        // Only auto-login if not coming from logout
        if (!isFromLogout) {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        }

        // Set click listeners
        btnLogin.setOnClickListener(v -> loginUser());
        
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        
        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
        
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Sign out first to clear any previously signed in account and force account selection dialog
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            createOrUpdateUserInDatabase(user);
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void createOrUpdateUserInDatabase(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        // Create user object - ensure phone number is always empty string for Google login
        User user = new User(userId, email, displayName, "");
        
        // Set profile image URL properly
        if (!photoUrl.isEmpty()) {
            user.setProfileImageUrl(photoUrl);
        }

        // Check if user already exists to preserve their phone number if they had one
        FirebaseUtils.getUserById(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get existing user data
                    User existingUser = dataSnapshot.getValue(User.class);
                    if (existingUser != null && existingUser.getPhoneNumber() != null) {
                        // If phone number exists and is not a URL (check if it contains http)
                        String phoneNumber = existingUser.getPhoneNumber();
                        if (phoneNumber != null && !phoneNumber.isEmpty() && !phoneNumber.contains("http")) {
                            // Keep the existing phone number
                            user.setPhoneNumber(phoneNumber);
                        }
                    }
                    
                    // Update user in the Realtime Database
                    FirebaseUtils.createOrUpdateUser(user, task -> {
                        // Now save to Firestore as well
                        saveUserToFirestore(user);
                    });
                } else {
                    // Create new user in the Realtime Database
                    FirebaseUtils.createOrUpdateUser(user, task -> {
                        // Now save to Firestore as well
                        saveUserToFirestore(user);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this,
                        "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void saveUserToFirestore(User user) {
        // Save user to Firestore
        FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create new user in Firestore
                        FirebaseFirestore.getInstance().collection("Users")
                                .document(user.getUserId())
                                .set(user)
                                .addOnSuccessListener(aVoid -> navigateToMainActivity())
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, 
                                            "Error creating user in Firestore: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                });
                    } else {
                        // Update existing user in Firestore if needed
                        // Only update fields that we have from Google Sign-in
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("userId", user.getUserId());
                        updates.put("email", user.getEmail());
                        
                        if (user.getName() != null && !user.getName().isEmpty()) {
                            updates.put("name", user.getName());
                        }
                        
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            updates.put("profileImageUrl", user.getProfileImageUrl());
                        }
                        
                        FirebaseFirestore.getInstance().collection("Users")
                                .document(user.getUserId())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> navigateToMainActivity())
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, 
                                            "Error updating user in Firestore: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                    navigateToMainActivity();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, 
                            "Error checking user in Firestore: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                });
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.required_field));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.required_field));
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Sign in with Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // Ensure user data is consistent across databases
                                syncUserData(user);
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Email not verified. Please verify your email and try again.",
                                        Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Login failed. User not found.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Email or Password is incorrect",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Syncs user data between Realtime Database and Firestore
    private void syncUserData(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        
        // Check if user exists in Realtime Database
        FirebaseUtils.getUserById(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user;
                if (dataSnapshot.exists()) {
                    // User exists in Realtime Database
                    user = dataSnapshot.getValue(User.class);
                    if (user == null) {
                        // Fallback if data is corrupted
                        user = new User(userId, email, "", "");
                    }
                } else {
                    // User doesn't exist in Realtime Database, create new user
                    user = new User(userId, email, "", "");
                    FirebaseUtils.createOrUpdateUser(user, task -> {
                        // Handled in saveUserToFirestore
                    });
                }
                
                // Now save to Firestore for consistency
                saveUserToFirestore(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this,
                        "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                // Continue to MainActivity despite error
                navigateToMainActivity();
            }
        });
    }

    private void checkUserInDatabase() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            String userId = currentUser.getUid();

            FirebaseUtils.getUserById(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // User doesn't exist in database, create new user
                        User user = new User(userId, currentUser.getEmail(), "", "");
                        FirebaseUtils.createOrUpdateUser(user, task -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(LoginActivity.this,
                            "Database error: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }else {
            Toast.makeText(this,"verify email to login",Toast.LENGTH_SHORT).show();
        }
    }

    private void showForgotPasswordDialog() {
        // For simplicity, directly get the email from the input field
        String email = etEmail.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Send password reset email
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, 
                                "Password reset email sent to " + email, 
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, 
                                "Error sending reset email: " + task.getException().getMessage(), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
} 