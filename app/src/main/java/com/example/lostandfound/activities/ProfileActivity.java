package com.example.lostandfound.activities;

import static android.content.ContentValues.TAG;
import static com.example.lostandfound.utils.ImageUtils.REQUEST_IMAGE_CAPTURE;
import static com.example.lostandfound.utils.ImageUtils.REQUEST_IMAGE_PICK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.ItemAdapter;
import com.example.lostandfound.adapters.ItemHistoryAdapter;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.models.User;
import com.example.lostandfound.utils.ImageUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvName, tvEmail, tvPhone, tvEmptyView;
    private Button btnEditProfile;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    private ItemHistoryAdapter itemAdapter;
    private List<Item> itemList;
    private User currentUser;
    private FirebaseFirestore firestore;
    private ImageView imgProfile;
    private Button btn_visibility;


    private String selectedImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        recyclerView = findViewById(R.id.recycler_view);
        tvEmptyView = findViewById(R.id.tv_empty_view);
        progressBar = findViewById(R.id.progress_bar);
        imgProfile = findViewById(R.id.iv_profile);
        // Initialize RecyclerView
        itemList = new ArrayList<>();
        itemAdapter = new ItemHistoryAdapter(this, itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemAdapter);

        loadUserData();
        loadUserPosts();

        btnEditProfile.setOnClickListener(v -> {
            showEditProfileDialog();
        });
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            
            // First try to load from Firestore
            firestore.collection("Users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUser = documentSnapshot.toObject(User.class);
                            if (currentUser != null) {
                                displayUserData(userId);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                // Fall back to Realtime Database if Firestore object is null
                                loadUserFromRealtimeDatabase(userId);
                            }
                        } else {
                            // If not in Firestore, try Realtime Database
                            loadUserFromRealtimeDatabase(userId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If Firestore fails, try Realtime Database
                        loadUserFromRealtimeDatabase(userId);
                    });
        } else {
            progressBar.setVisibility(View.GONE);
            tvEmail.setText("No user signed in.");
        }
    }
    
    private void loadUserFromRealtimeDatabase(String userId) {
        // Try to get user data from Realtime Database as fallback
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                .child(userId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (dataSnapshot.exists()) {
                        currentUser = dataSnapshot.getValue(User.class);
                        if (currentUser != null) {
                            // Also save to Firestore for future consistency
                            firestore.collection("Users").document(userId)
                                    .set(currentUser);
                            displayUserData(userId);
                        } else {
                            Toast.makeText(ProfileActivity.this, "Unable to load user data", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Last resort - create minimal user object from Firebase Auth
                        FirebaseUser authUser = auth.getCurrentUser();
                        if (authUser != null) {
                            currentUser = new User(
                                    authUser.getUid(),
                                    authUser.getEmail(),
                                    authUser.getDisplayName() != null ? authUser.getDisplayName() : "",
                                    ""
                            );
                            if (authUser.getPhotoUrl() != null) {
                                currentUser.setProfileImageUrl(authUser.getPhotoUrl().toString());
                            }
                            
                            // Save this basic profile to both databases for future
                            firestore.collection("Users").document(userId).set(currentUser);
                            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                                    .child(userId).setValue(currentUser);
                            
                            displayUserData(userId);
                        } else {
                            Toast.makeText(ProfileActivity.this, "Unable to load user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayUserData(String userId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        
        // Fix phone number if it's storing a URL
        String phoneNumber = currentUser.getPhoneNumber();
        if (phoneNumber != null && phoneNumber.contains("http")) {
            // If phone number is a URL, it's likely the profile image URL incorrectly stored
            // Move the URL to profileImageUrl if it's empty and clear the phone number
            if (currentUser.getProfileImageUrl() == null || currentUser.getProfileImageUrl().isEmpty()) {
                currentUser.setProfileImageUrl(phoneNumber);
            }
            // Clear the phone number
            currentUser.setPhoneNumber("");
            
            // Update both databases
            firestore.collection("Users").document(userId)
                    .update("phoneNumber", "", "profileImageUrl", currentUser.getProfileImageUrl());
            
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId)
                    .child("phoneNumber")
                    .setValue("");
            
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId)
                    .child("profileImageUrl")
                    .setValue(currentUser.getProfileImageUrl());
        }
        
        tvName.setText(currentUser.getName());
        if (user != null) {
            String email = user.getEmail();
            tvEmail.setText(email);
        } else {
            tvEmail.setText("No user is signed in.");
        }
        tvPhone.setText(currentUser.getPhoneNumber());

        if (imgProfile != null) {
            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                // Ensure ImageView updates only if the user is the same
                imgProfile.setTag(userId);
                Glide.with(this)
                    .load(currentUser.getProfileImageUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(imgProfile);
            } else {
                imgProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        }
    }


    private void loadUserPosts() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference itemsRef = db.collection("Users").document(userId).collection("Items");

            itemsRef.get().addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    itemList.clear(); // Clear existing list

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Item item = document.toObject(Item.class);
                        item.setItemId(document.getId()); // Set the itemId manually if needed
                        itemList.add(item);
                    }

                    // Sort by timestamp (newest first)
                    Collections.sort(itemList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    updateUI();
                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Error loading items: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateUI() {
        if (itemList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyView.setVisibility(View.GONE);
            itemAdapter.notifyDataSetChanged();
        }
    }

    private void showEditProfileDialog() {
        onPause();
        if (currentUser == null) {
            Toast.makeText(this, "Unable to load user data", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(view);

        TextInputEditText etName = view.findViewById(R.id.et_name);
        TextInputEditText etEmail = view.findViewById(R.id.et_email);
        TextInputEditText etOldPassword = view.findViewById(R.id.et_password_old);
        TextInputEditText etNewPassword = view.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnUpdate = view.findViewById(R.id.btn_update);

        visibility(view);
        etName.setText(currentUser.getName());
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser userEmail = auth.getCurrentUser();
        tvName.setText(currentUser.getName());

        if (userEmail != null) {
            String email = userEmail.getEmail();
            etEmail.setText(email);
        } else {
            etEmail.setText("No user is signed in.");
        }

        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                .load(currentUser.getProfileImageUrl())
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .into(imgProfile);
        } else {
            imgProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        imgProfile.setOnClickListener(v -> ImageUtils.showImagePickerDialog(this, this));

        AlertDialog dialog = builder.create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError(getString(R.string.required_field));
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError(getString(R.string.required_field));
                return;
            }

            if (!oldPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty()) {
                if (oldPassword.isEmpty()) {
                    etOldPassword.setError(getString(R.string.required_field));
                    return;
                }
                if (newPassword.isEmpty()) {
                    etNewPassword.setError(getString(R.string.required_field));
                    return;
                }
                if (!newPassword.equals(confirmPassword)) {
                    etConfirmPassword.setError("Passwords do not match");
                    return;
                }
            }

            progressBar.setVisibility(View.VISIBLE);
            btnUpdate.setEnabled(false);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (!email.equals(userEmail.getEmail())) {
                if (oldPassword.isEmpty()) {
                    etOldPassword.setError(getString(R.string.required_field));
                    progressBar.setVisibility(View.GONE);
                    btnUpdate.setEnabled(true);
                    return;
                }

                // Reauthenticate before changing email
                AuthCredential credential = EmailAuthProvider.getCredential(userEmail.getEmail(), oldPassword);
                user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        // Send verification before updating the email
                        user.verifyBeforeUpdateEmail(email).addOnCompleteListener(verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Verification email sent to " + email, Toast.LENGTH_LONG).show();
                                FirebaseAuth.getInstance().signOut();
                                dialog.dismiss();

                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                handleFailure(verificationTask.getException(), "Failed to send verification email");
                                progressBar.setVisibility(View.GONE);
                                btnUpdate.setEnabled(true);
                            }
                        });
                    } else {
                        handleFailure(authTask.getException(), "Reauthentication failed");
                        progressBar.setVisibility(View.GONE);
                        btnUpdate.setEnabled(true);
                    }
                });
            } else {
                updatePasswordAndProfile(user, newPassword, name, email, progressBar, btnUpdate, dialog);
            }
        });

        dialog.show();
    }

    private void handleFailure(Exception exception, String message) {
        Log.e(TAG, message + ": " + exception.getMessage());
        Toast.makeText(ProfileActivity.this, message + ": " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }


    private void updatePasswordAndProfile(FirebaseUser user, String newPassword, String name, String email, ProgressBar progressBar, Button button, AlertDialog dialog) {
        if (!newPassword.isEmpty()) {
            user.updatePassword(newPassword).addOnCompleteListener(passwordTask -> {
                if (passwordTask.isSuccessful()) {
                    updateProfileInFirestore(name, email, progressBar, button, dialog);
                } else {
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Failed to update password: " + passwordTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            updateProfileInFirestore(name, email, progressBar, button, dialog);
        }
    }

    private void updateProfileInFirestore(String name, String email, ProgressBar progressBar, Button button, AlertDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("Users").document(currentUser.getUserId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("profileImageUrl", selectedImageUrl);

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update name in all items created by this user
                    updateUserNameInAllItems(currentUser.getUserId(), name);
                    
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);

                    currentUser.setName(name);
                    currentUser.setEmail(email);
                    currentUser.setProfileImageUrl(selectedImageUrl);

                    Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    loadUserData();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to update userName in all items created by this user
    private void updateUserNameInAllItems(String userId, String newName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Also update in Realtime Database for compatibility
        com.google.firebase.database.DatabaseReference itemsRef = 
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Items");
        
        // 1. Update items in the main "All Items" collection
        db.collection("All Items")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = 0;
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    document.getReference().update("userName", newName)
                        .addOnSuccessListener(aVoid -> 
                            Log.d("ProfileActivity", "Successfully updated item: " + document.getId()))
                        .addOnFailureListener(e -> 
                            Log.e("ProfileActivity", "Failed to update item: " + document.getId() + ", error: " + e.getMessage()));
                    count++;
                }
                Log.d("ProfileActivity", "Found " + count + " items to update in All Items collection");
                
                // If no items were found, try to search without filters in case the userId field is inconsistent
                if (count == 0) {
                    Log.d("ProfileActivity", "No items found with userId filter, checking all items");
                    searchAllItems(db, userId, newName);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileActivity", "Error querying items: " + e.getMessage());
                // Try alternate approach if the main one failed
                searchAllItems(db, userId, newName);
            });
            
        // 2. Update items in user's own "Items" subcollection
        db.collection("Users").document(userId)
            .collection("Items")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = 0;
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    document.getReference().update("userName", newName)
                        .addOnSuccessListener(aVoid -> 
                            Log.d("ProfileActivity", "Successfully updated user item: " + document.getId()))
                        .addOnFailureListener(e -> 
                            Log.e("ProfileActivity", "Failed to update user item: " + document.getId() + ", error: " + e.getMessage()));
                    count++;
                }
                Log.d("ProfileActivity", "Updated userName in " + count + " user items");
            })
            .addOnFailureListener(e -> 
                Log.e("ProfileActivity", "Error updating userName in user items: " + e.getMessage())
            );
            
        // 3. Update items in Realtime Database as well
        itemsRef.orderByChild("userId").equalTo(userId)
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                int count = 0;
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().child("userName").setValue(newName);
                        count++;
                    }
                }
                Log.d("ProfileActivity", "Updated " + count + " items in Realtime Database");
            })
            .addOnFailureListener(e -> 
                Log.e("ProfileActivity", "Error updating Realtime Database items: " + e.getMessage())
            );
    }
    
    // Helper method to search all items for a given user ID
    private void searchAllItems(FirebaseFirestore db, String userId, String newName) {
        db.collection("All Items")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int count = 0;
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    // Check if this document belongs to the user
                    if (document.contains("userId") && userId.equals(document.getString("userId"))) {
                        document.getReference().update("userName", newName);
                        count++;
                    }
                }
                Log.d("ProfileActivity", "Updated " + count + " items through full collection scan");
            })
            .addOnFailureListener(e -> 
                Log.e("ProfileActivity", "Error in full collection scan: " + e.getMessage())
            );
    }

    private void visibility(View view) {
        TextInputLayout til_password_old = view.findViewById(R.id.til_password_old);
        TextInputLayout til_password_new = view.findViewById(R.id.til_new_password);
        TextInputLayout til_password_confirm = view.findViewById(R.id.til_comfirm_password);
        TextInputLayout til_name = view.findViewById(R.id.til_name);
        TextInputLayout til_pemail = view.findViewById(R.id.til_pemail);
        CardView img_card = view.findViewById(R.id.img_card);
        btn_visibility = view.findViewById(R.id.password_visibility);

        imgProfile = view.findViewById(R.id.iv_edit);
        btn_visibility.setOnClickListener(v -> {
            til_password_old.setVisibility(View.VISIBLE);
            til_password_new.setVisibility(View.VISIBLE);
            til_password_confirm.setVisibility(View.VISIBLE);
            img_card.setVisibility(View.GONE);
            til_name.setVisibility(View.GONE);
            til_pemail.setVisibility(View.GONE);
            btn_visibility.setVisibility(View.GONE);

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri imageUri = null;

            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = ImageUtils.getImageUriFromBitmap(this, photo);
            }

            if (imageUri != null) {
                String userId = currentUser.getUserId();
                Uri finalImageUri = imageUri;
                ImageUtils.uploadImageToFirebase(this, finalImageUri, userId, new ImageUtils.ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        selectedImageUrl = imageUrl;
                        // Load image with Glide
                        Glide.with(ProfileActivity.this)
                            .load(imageUrl)
                            .placeholder(android.R.drawable.ic_menu_myplaces)
                            .error(android.R.drawable.ic_menu_myplaces)
                            .into(imgProfile);
                        
                        // Immediately update the profile image URL in Firestore
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("profileImageUrl", imageUrl);
                            
                            firestore.collection("Users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Update the current user object as well
                                    if (currentUser != null) {
                                        currentUser.setProfileImageUrl(imageUrl);
                                        // Make sure the UI is refreshed with the new image
                                        displayUserData(userId);
                                    }
                                    Toast.makeText(ProfileActivity.this, "Profile image updated", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ProfileActivity.this, 
                                        "Failed to update profile image: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                });
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    


}
