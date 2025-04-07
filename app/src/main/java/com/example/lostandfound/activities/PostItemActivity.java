package com.example.lostandfound.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.models.User;
import com.example.lostandfound.utils.ImageUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;

public class PostItemActivity extends AppCompatActivity {

    private static final String DATE_FORMAT = "MMM dd, yyyy";
    
    private Toolbar toolbar;
    private RadioGroup radioGroupType;
    private ImageView ivItemImage;
    private Button btnAddPhoto, btnSubmit;
    private TextInputEditText etTitle, etDescription, etLocation, etDate, etPhone;
    private Spinner categorySpinner;
    private ProgressBar progressBar;
    private Date returnDate = null;
    private Date date = new Date();
    private Uri imageUri;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private User currentUser;
    private String selectedCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);
        
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize views
        radioGroupType = findViewById(R.id.radio_group_type);
        ivItemImage = findViewById(R.id.iv_item_image);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        etTitle = findViewById(R.id.et_title);
        categorySpinner = findViewById(R.id.category_spinner);
        etDescription = findViewById(R.id.et_description);
        etLocation = findViewById(R.id.et_location);
        etDate = findViewById(R.id.et_date);
        etPhone = findViewById(R.id.et_phone);
        btnSubmit = findViewById(R.id.btn_submit);
        progressBar = findViewById(R.id.progress_bar);
        
        // Initialize date picker
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        etDate.setText(dateFormat.format(selectedDate.getTime()));

        // Load current user data
        loadCurrentUserData();

        // Initialize category spinner
        setupCategorySpinner();
        
        btnAddPhoto.setOnClickListener(v ->
                ImageUtils.showImagePickerDialog(this, this)
        );
        
        etDate.setOnClickListener(v -> showDatePicker());
        
        btnSubmit.setOnClickListener(v -> validateAndUploadItem());
    }
    
    private void loadCurrentUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            progressBar.setVisibility(View.VISIBLE);
            
            FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        progressBar.setVisibility(View.GONE);
                        if (documentSnapshot.exists()) {
                            currentUser = documentSnapshot.toObject(User.class);
                            if (currentUser != null) {
                                // Pre-fill phone number if available
                                if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                                    etPhone.setText(currentUser.getPhoneNumber());
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Log.e("PostItemActivity", "Error loading user data: " + e.getMessage());
                    });
        }
    }
    
    private void setupCategorySpinner() {
        // Create an adapter without the "All Categories" option
        ArrayAdapter<CharSequence> originalAdapter = ArrayAdapter.createFromResource(
                this, 
                R.array.category_array, 
                android.R.layout.simple_spinner_item);
        
        // Create a new adapter without the first item ("All Categories")
        ArrayAdapter<String> filteredAdapter = new ArrayAdapter<>(
                this, 
                android.R.layout.simple_spinner_item);
        
        // Add all categories except "All Categories"
        for (int i = 1; i < originalAdapter.getCount(); i++) {
            filteredAdapter.add(originalAdapter.getItem(i).toString());
        }
        
        filteredAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(filteredAdapter);
        
        // Select the first category by default
        if (filteredAdapter.getCount() > 0) {
            categorySpinner.setSelection(0);
        }
        
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Select first category as fallback
                if (filteredAdapter.getCount() > 0) {
                    selectedCategory = filteredAdapter.getItem(0).toString();
                } else {
                    selectedCategory = "";
                }
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == ImageUtils.REQUEST_IMAGE_CAPTURE && data != null) {
                // 📸 Handle Camera Image
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivItemImage.setImageBitmap(imageBitmap); // Show the image in ImageView
                    imageUri = ImageUtils.getImageUriFromBitmap(this, imageBitmap); // Convert Bitmap to URI
                }
            } else if (requestCode == ImageUtils.REQUEST_IMAGE_PICK && data != null) {
                // 🖼️ Handle Gallery Image
                imageUri = data.getData();
                ivItemImage.setImageURI(imageUri);
            }
        }
    }

    public void validateAndUploadItem() {
        String title = etTitle.getText().toString().trim();
        String category = selectedCategory;
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        int selectedId = radioGroupType.getCheckedRadioButtonId();
        String itemType;

        if (selectedId == R.id.radio_lost) {
            itemType = "Lost";
        } else if (selectedId == R.id.radio_found) {
            itemType = "Found";
        } else {
            Toast.makeText(this, "Please select item type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(title)) {
            etTitle.setError(getString(R.string.required_field));
            return;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etDescription.setError(getString(R.string.required_field));
            return;
        }
        if (TextUtils.isEmpty(location)) {
            etLocation.setError(getString(R.string.required_field));
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError(getString(R.string.required_field));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "You must be logged in to post an item", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String userName = currentUser != null ? currentUser.getName() : "";

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String productId = firestore.collection("All Items").document().getId(); // Generate unique ID

        returnDate = null;
        // Use the date selected in the date picker instead of current date
        date = selectedDate.getTime();
        
        // Use the selected category from the spinner
        Item item = new Item(null, title, description, category, location, date, returnDate, itemType, "", userId, userName, phone);
        item.setItemId(productId); // Assign generated ID
        
        // Set timestamp for proper sorting
        item.setTimestamp(System.currentTimeMillis());

        if (imageUri != null) {
            // Upload image using itemId instead of userId
            ImageUtils.uploadImageToFirebase(this, imageUri, productId, new ImageUtils.ImageUploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    item.setImageUrl(imageUrl);
                    uploadItemToAllItems(item);
                    uploadItemToUserItems(userId,item);
                }

                @Override
                public void onFailure(String errorMessage) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PostItemActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            uploadItemToAllItems(item);
            uploadItemToUserItems(userId,item);
        }
    }

    private void uploadItemToUserItems(String userId, Item item) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String productId = item.getItemId();
        firestore.collection("Users").document(userId)
                .collection("Items")
                .document(productId)
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Item added to user's collection ('Items' subcollection) with 'Not Returned'");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding item to user's collection: " + e.getMessage());
                });
    }

    // Function to upload item to the "All Items" collection without modifying itemType
    private void uploadItemToAllItems(Item item) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String productId = item.getItemId(); // Use the generated item ID

        firestore.collection("All Items")
                .document(productId)
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Item uploaded successfully", Toast.LENGTH_SHORT).show();
                    
                    // Add a slight delay before finishing to show the success message
                    new android.os.Handler().postDelayed(() -> {
                        // Return to MainActivity (All Items screen)
                        Intent intent = new Intent(PostItemActivity.this, com.example.lostandfound.MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish(); // Close this activity
                    }, 1000); // 1 second delay
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error adding item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


     void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                new ContextThemeWrapper(this, R.style.CustomDatePickerStyle),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        
        // Customize the header text color
        try {
            // Try to adjust the header color 
            int headerId = Resources.getSystem().getIdentifier("date_picker_header", "id", "android");
            if (headerId != 0) {
                View header = datePickerDialog.findViewById(headerId);
                if (header != null) {
                    header.setBackgroundColor(getResources().getColor(R.color.primary));
                }
            }
        } catch (Exception e) {
            // Ignore if this fails
            Log.e("DatePickerCustomization", "Error customizing header", e);
        }
        
        datePickerDialog.show();
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