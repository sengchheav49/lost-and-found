package com.example.lostandfound.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.FirebaseUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {
    
    private static final String DATE_FORMAT = "MMM dd, yyyy";
    
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivItemImage;
    private TextView tvItemType, tvItemDate, tvItemTitle, tvItemCategory;
    private TextView tvItemLocation, tvItemDescription, tvPosterName;
    private Button btnCall, btnMessage;
    private ProgressBar progressBar;
    
    private String itemId;
    private String userId;
    private Item currentItem;
    private boolean isCurrentUserPost = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ivItemImage = findViewById(R.id.iv_item_image);
        tvItemType = findViewById(R.id.tv_item_type);
        tvItemDate = findViewById(R.id.tv_item_date);
        tvItemTitle = findViewById(R.id.tv_item_titles);
        tvItemCategory = findViewById(R.id.tv_item_category);
        tvItemLocation = findViewById(R.id.tv_item_location);
        tvItemDescription = findViewById(R.id.tv_item_description);
        tvPosterName = findViewById(R.id.tv_poster_name);
        btnCall = findViewById(R.id.btn_call);
        btnMessage = findViewById(R.id.btn_message);
        progressBar = findViewById(R.id.progress_bar);

        // Get data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            itemId = intent.getStringExtra("itemId");
            userId = intent.getStringExtra("userId");
            String title = intent.getStringExtra("title");
            String category = intent.getStringExtra("category");
            String location = intent.getStringExtra("location");
            String date = intent.getStringExtra("date");
            String itemType = intent.getStringExtra("itemType");
            String imageUrl = intent.getStringExtra("imageUrl");
            String description = intent.getStringExtra("description");
            String contactInfo = intent.getStringExtra("contactInfo");
            String userName = intent.getStringExtra("userName");

            // Check if this post belongs to the current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && userId != null && userId.equals(currentUser.getUid())) {
                isCurrentUserPost = true;
                invalidateOptionsMenu(); // Refresh the options menu
            }

            collapsingToolbar.setTitle(" ");
            tvItemTitle.setText(title);
            tvItemCategory.setText(category);
            tvItemLocation.setText(location);
            tvItemDate.setText(date);
            tvItemType.setText(itemType.toUpperCase());
            tvItemDescription.setText(description);
            
            // Display user name if available
            if (userName != null && !userName.isEmpty()) {
                tvPosterName.setText(userName);
                tvPosterName.setVisibility(View.VISIBLE);
            } else {
                tvPosterName.setText("Anonymous");
                tvPosterName.setVisibility(View.VISIBLE);
            }

            // Set item type background color
            int backgroundColorRes = itemType.equalsIgnoreCase("lost") ?
                    R.style.LostItemLabel : R.style.FoundItemLabel;
            tvItemType.setTextAppearance(this, backgroundColorRes);

            // Load image using Picasso
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.circle_background)
                        .error(R.drawable.circle_background)
                        .centerCrop()
                        .into(ivItemImage);
            } else {
                ivItemImage.setImageResource(R.drawable.circle_background);
            }
        } else {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set click listeners for contact buttons
        btnCall.setOnClickListener(v -> makePhoneCall());
        btnMessage.setOnClickListener(v -> sendSMS());
    }
    
    private void loadItemFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
            .collection("All Items")
            .document(itemId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentItem = documentSnapshot.toObject(Item.class);
                    
                    // Get the latest user name from the user's profile
                    if (currentItem != null && currentItem.getUserId() != null) {
                        FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(currentItem.getUserId())
                            .get()
                            .addOnSuccessListener(userDocument -> {
                                progressBar.setVisibility(View.GONE);
                                
                                if (userDocument.exists()) {
                                    // Get the latest user name
                                    String latestUserName = userDocument.getString("name");
                                    
                                    if (latestUserName != null && !latestUserName.isEmpty()) {
                                        // Update the user name in our local object
                                        currentItem.setUserName(latestUserName);
                                        
                                        // Also update it in the database for consistency
                                        if (!latestUserName.equals(currentItem.getUserName())) {
                                            documentSnapshot.getReference()
                                                .update("userName", latestUserName)
                                                .addOnSuccessListener(aVoid -> 
                                                    Log.d("ItemDetailActivity", "Updated user name in item"))
                                                .addOnFailureListener(e -> 
                                                    Log.e("ItemDetailActivity", "Failed to update user name: " + e.getMessage()));
                                        }
                                    }
                                }
                                updateUI();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Log.e("ItemDetailActivity", "Error fetching user details: " + e.getMessage());
                                updateUI();
                            });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        updateUI();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Item no longer exists", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error loading item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void updateUI() {
        if (currentItem == null) return;
        
        tvItemTitle.setText(currentItem.getTitle());
        tvItemCategory.setText(currentItem.getCategory());
        tvItemLocation.setText(currentItem.getLocation());
        tvItemDescription.setText(currentItem.getDescription());
        
        if (currentItem.getDate() != null) {
            String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(currentItem.getDate());
            tvItemDate.setText(date);
        }
        
        tvItemType.setText(currentItem.getItemType().toUpperCase());
        
        // Set item type style
        int backgroundStyle = currentItem.getItemType().equalsIgnoreCase("lost") ?
                R.style.LostItemLabel : R.style.FoundItemLabel;
        tvItemType.setTextAppearance(this, backgroundStyle);
        
        // Display user name if available
        if (currentItem.getUserName() != null && !currentItem.getUserName().isEmpty()) {
            tvPosterName.setText(currentItem.getUserName());
            tvPosterName.setVisibility(View.VISIBLE);
        } else {
            tvPosterName.setText("Anonymous");
            tvPosterName.setVisibility(View.VISIBLE);
        }
        
        // Load image using Picasso
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentItem.getImageUrl())
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .centerCrop()
                    .into(ivItemImage);
        } else {
            ivItemImage.setImageResource(R.drawable.circle_background);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only inflate the menu if this is the user's own post
        if (isCurrentUserPost) {
            getMenuInflater().inflate(R.menu.detail_menu, menu);
        }
        return true;
    }

    private void makePhoneCall() {
        String phoneNumber = getIntent().getStringExtra("contactInfo");

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            if (currentItem != null && currentItem.getUserPhoneNumber() != null && !currentItem.getUserPhoneNumber().isEmpty()) {
                phoneNumber = currentItem.getUserPhoneNumber();
            }
        }

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS() {
        String phoneNumber = getIntent().getStringExtra("contactInfo");

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            if (currentItem != null && currentItem.getUserPhoneNumber() != null && !currentItem.getUserPhoneNumber().isEmpty()) {
                phoneNumber = currentItem.getUserPhoneNumber();
            }
        }

        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            intent.putExtra("sms_body", "Hello, I'm contacting you about your " +
                    (currentItem != null ? currentItem.getItemType() + " item: " + currentItem.getTitle() : "item"));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Item was edited successfully, reload the data
            loadItemFromFirestore();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload item data when returning to this activity (after editing)
        // Only needed as a fallback in case onActivityResult doesn't catch it
        loadItemFromFirestore();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_edit) {
            // Start the edit activity
            if (isCurrentUserPost) {
                Intent intent = new Intent(this, EditItemActivity.class);
                intent.putExtra("itemId", itemId);
                startActivityForResult(intent, 100); // Using request code 100
                return true;
            } else {
                Toast.makeText(this, "You can only edit your own posts", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
} 