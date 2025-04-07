package com.example.lostandfound.activities;

import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.bumptech.glide.Glide;
import java.util.Date;
import java.util.Locale;


public class History_Details extends AppCompatActivity {
    private ImageView ivItemImage, imageButton;
    private TextView tvItemDate, tvItemTitle, contact,pickup_date_bold,tv_date_returned;
    private ConstraintLayout return_tv,return_date_view;
    private TextView tvItemLocation, tvItemPickupLocation, tvItemDescription, found_address_bold;
    private String itemId;
    private Button btnReturn;
    private String itemType,title,location,getDate,description,contactInfo;
    private String imageUrl;
    Date date;
    private long returnDateMillis;
    String category;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_details);

        findViewById();
        getArguments();
        displayImage();
        backPressed();
    }

    private void findViewById() {
        ivItemImage = findViewById(R.id.iv_item_image_history);
        tvItemTitle = findViewById(R.id.tv_item_title_history);
        tvItemLocation = findViewById(R.id.found_address);
        tvItemDate = findViewById(R.id.tv_date);
        contact = findViewById(R.id.pickup_contact);
        tvItemPickupLocation = findViewById(R.id.tv_pickup_address_history);
        tvItemDescription = findViewById(R.id.tv_item_description_history);
        imageButton = findViewById(R.id.imageButton);
        btnReturn = findViewById(R.id.return_button);
        return_tv = findViewById(R.id.return_textview);
        found_address_bold = findViewById(R.id.found_address_bold);
        //pickup_date_bold = findViewById(R.id.pickup_date_bold);
        tv_date_returned = findViewById(R.id.tv_date_returned);
        return_date_view = findViewById(R.id.constraint_returned_date_view);
    }

    private void getArguments() {
        Intent intent = getIntent();
        if (intent != null) {
            itemId = intent.getStringExtra("itemId");
            title = intent.getStringExtra("title");
            location = intent.getStringExtra("location");
            date = (Date) getIntent().getSerializableExtra("date");
            //date_returned = (Date) getIntent().getSerializableExtra("returnDate");
            long date_returned = getIntent().getLongExtra("returnDate", -1);
            category = intent.getStringExtra("category");
            imageUrl = intent.getStringExtra("imageUrl");
            description = intent.getStringExtra("description");
            contactInfo = intent.getStringExtra("contactInfo");
            itemType = intent.getStringExtra("itemType");
            String getDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
            long returnDateMillis = getIntent().getLongExtra("returnDate", -1L);
            Date returnDate = (returnDateMillis != -1L) ? new Date(returnDateMillis) : null;

            if (returnDate != null) {
                String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(returnDate);
                tv_date_returned.setText(formattedDate);
                return_date_view.setVisibility(View.VISIBLE);
            } else {
                return_date_view.setVisibility(View.GONE);
            }

            tvItemTitle.setText(title);
            tvItemLocation.setText(location);
            tvItemDate.setText(getDate);
            tvItemDescription.setText(description);
            contact.setText(contactInfo);
            tvItemPickupLocation.setText(location);



            // Handle the button visibility based on item type
            if (itemType != null && itemType.equalsIgnoreCase("Returned")) {
                btnReturn.setVisibility(View.GONE);
                return_tv.setVisibility(View.VISIBLE);
            } else {
                btnReturn.setVisibility(View.VISIBLE);
                return_tv.setVisibility(View.GONE);
                returnItem();
            }
        }


    }
    private void returnItem() {
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date currentDate = new Date();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String userId = user.getUid();
                String productId = itemId;
                Item item = new Item(itemId, title, description, category, location, date, currentDate, itemType, imageUrl, userId, contactInfo);
                item.setItemId(productId);
                
                // First, delete from All Items collection
                deleteFromAllItems(productId);
                
                // Then, update in user's items collection
                uploadItemToUserItems(userId, item);
            }
        });
    }

    // New method to delete item from All Items collection
    private void deleteFromAllItems(String itemId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("All Items")
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Item successfully deleted from All Items");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting item from All Items: " + e.getMessage());
                });
    }

    private void displayImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(ivItemImage);
        } else {
            ivItemImage.setImageResource(R.drawable.circle_background);
        }
    }

    private void backPressed() {
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }



    private void uploadItemToUserItems(String userId, Item item) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String productId = item.getItemId();
        item.setItemType("Returned");
        firestore.collection("Users").document(userId)
                .collection("Items")
                .document(productId)
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,"Item Returned successfully",Toast.LENGTH_SHORT).show();
                    return_tv.setVisibility(View.VISIBLE);
                    btnReturn.setVisibility(View.GONE);
                    tv_date_returned.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()));
                    return_date_view.setVisibility(View.VISIBLE);
                    
                    // Add a slight delay before finishing to show the success message
                    new android.os.Handler().postDelayed(() -> {
                        // Return to MainActivity (All Items screen)
                        Intent intent = new Intent(History_Details.this, com.example.lostandfound.MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish(); // Close this activity
                    }, 1500); // 1.5 second delay
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,"Item Returned failed",Toast.LENGTH_SHORT).show();
                });
    }
}
