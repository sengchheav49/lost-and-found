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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.ImageUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditItemActivity extends AppCompatActivity {

    private static final String TAG = "EditItemActivity";
    private static final String DATE_FORMAT = "MMM dd, yyyy";
    
    private Toolbar toolbar;
    private RadioGroup radioGroupType;
    private RadioButton radioLost, radioFound;
    private ImageView ivItemImage;
    private Button btnAddPhoto, btnSubmit;
    private TextInputEditText etTitle, etDescription, etLocation, etDate, etPhone;
    private Spinner categorySpinner;
    private ProgressBar progressBar;
    
    private Date date = new Date();
    private Uri imageUri;
    private String currentImageUrl;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private String selectedCategory = "";
    private String itemId;
    private Item currentItem;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);
        
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Item");
        }
        
        // Initialize views
        radioGroupType = findViewById(R.id.radio_group_type);
        radioLost = findViewById(R.id.radio_lost);
        radioFound = findViewById(R.id.radio_found);
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
        
        // Setup category spinner
        setupCategorySpinner();
        
        // Get item ID from intent
        itemId = getIntent().getStringExtra("itemId");
        
        if (itemId == null) {
            Toast.makeText(this, "Error: Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load item data
        loadItemData();
        
        btnAddPhoto.setOnClickListener(v ->
                ImageUtils.showImagePickerDialog(this, this)
        );
        
        etDate.setOnClickListener(v -> showDatePicker());
        
        btnSubmit.setText("Update");
        btnSubmit.setOnClickListener(v -> validateAndUpdateItem());
    }
    
    private void loadItemData() {
        progressBar.setVisibility(View.VISIBLE);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("All Items").document(itemId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentItem = documentSnapshot.toObject(Item.class);
                        populateFormWithItemData();
                    } else {
                        Toast.makeText(EditItemActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditItemActivity.this, "Error loading item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
    
    private void populateFormWithItemData() {
        if (currentItem == null) return;
        
        // Set item type
        if ("Lost".equalsIgnoreCase(currentItem.getItemType())) {
            radioLost.setChecked(true);
        } else if ("Found".equalsIgnoreCase(currentItem.getItemType())) {
            radioFound.setChecked(true);
        }
        
        // Set title and description
        etTitle.setText(currentItem.getTitle());
        etDescription.setText(currentItem.getDescription());
        etLocation.setText(currentItem.getLocation());
        etPhone.setText(currentItem.getUserPhoneNumber());
        
        // Set date
        if (currentItem.getDate() != null) {
            date = currentItem.getDate();
            selectedDate.setTime(date);
            etDate.setText(dateFormat.format(date));
        }
        
        // Set category
        String category = currentItem.getCategory();
        selectCategoryInSpinner(category);
        
        // Load image
        currentImageUrl = currentItem.getImageUrl();
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentImageUrl)
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background)
                    .into(ivItemImage);
        }
    }
    
    private void selectCategoryInSpinner(String category) {
        ArrayAdapter adapter = (ArrayAdapter) categorySpinner.getAdapter();
        if (adapter != null && category != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equalsIgnoreCase(category)) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
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
    
    private void validateAndUpdateItem() {
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
            Toast.makeText(this, "You must be logged in to update an item", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the current item with new values
        if (currentItem != null) {
            currentItem.setTitle(title);
            currentItem.setCategory(category);
            currentItem.setDescription(description);
            currentItem.setLocation(location);
            currentItem.setUserPhoneNumber(phone);
            currentItem.setItemType(itemType);
            
            // Set the date from the date picker
            currentItem.setDate(selectedDate.getTime());
            
            // If user changed the image, upload the new one
            if (imageUri != null) {
                // Upload image using itemId
                ImageUtils.uploadImageToFirebase(this, imageUri, itemId, new ImageUtils.ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        currentItem.setImageUrl(imageUrl);
                        saveUpdatedItem();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditItemActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // No change to image, just update other fields
                saveUpdatedItem();
            }
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: Item not loaded properly", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveUpdatedItem() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String userId = currentItem.getUserId();
        
        // Update in All Items collection
        firestore.collection("All Items")
                .document(itemId)
                .set(currentItem)
                .addOnSuccessListener(aVoid -> {
                    // Update in user's collection
                    firestore.collection("Users").document(userId)
                            .collection("Items")
                            .document(itemId)
                            .set(currentItem)
                            .addOnSuccessListener(aVoid1 -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(EditItemActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                                
                                // Set result as successful
                                setResult(RESULT_OK);
                                
                                // Return to detail view with a delay
                                new android.os.Handler().postDelayed(() -> {
                                    finish();
                                }, 1000);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Error updating user item: " + e.getMessage());
                                Toast.makeText(EditItemActivity.this, "Item updated in main collection but failed in user collection", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditItemActivity.this, "Error updating item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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