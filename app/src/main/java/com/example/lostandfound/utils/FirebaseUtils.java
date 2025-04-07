package com.example.lostandfound.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lostandfound.models.Item;
import com.example.lostandfound.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class FirebaseUtils {
    private static final String TAG = "FirebaseUtils";
    
    // Firebase Database references
    private static final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private static final DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("Items");
    
    // Firebase Storage reference
    private static final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    
    // Firebase Authentication
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    
    // Get current user
    public static FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    // Get current user ID
    public static String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    // Create or update user in the database
    public static void createOrUpdateUser(User user, final OnCompleteListener<Void> onCompleteListener) {
        usersRef.child(user.getUserId()).setValue(user)
                .addOnCompleteListener(onCompleteListener);
    }
    
    // Get user by ID
    public static void getUserById(String userId, final ValueEventListener listener) {
        usersRef.child(userId).addListenerForSingleValueEvent(listener);
    }
    
    // Create a new item
    public static void createItem(Item item, final OnCompleteListener<Void> onCompleteListener) {
        String itemId = itemsRef.push().getKey();
        item.setItemId(itemId);
        
        itemsRef.child(itemId).setValue(item)
                .addOnCompleteListener(onCompleteListener);
    }
    
    // Get all items
    public static void getAllItems(final ValueEventListener listener) {
        itemsRef.orderByChild("timestamp").addValueEventListener(listener);
    }
    
    // Get items by type (lost or found)
    public static void getItemsByType(String type, final ValueEventListener listener) {
        itemsRef.orderByChild("itemType").equalTo(type).addValueEventListener(listener);
    }
    
    // Get items by user
    public static void getItemsByUser(String userId, final ValueEventListener listener) {
        itemsRef.orderByChild("userId").equalTo(userId).addValueEventListener(listener);
    }
    
    // Delete an item
    public static void deleteItem(String itemId, final OnCompleteListener<Void> onCompleteListener) {
        // First get the item to check if it has an image to delete
        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Item item = dataSnapshot.getValue(Item.class);
                if (item != null && item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    // Delete the image from storage
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(item.getImageUrl());
                    imageRef.delete().addOnSuccessListener(aVoid -> {
                        // Now delete the item from the database
                        itemsRef.child(itemId).removeValue().addOnCompleteListener(onCompleteListener);
                    }).addOnFailureListener(e -> {
                        // If image deletion fails, still delete the item
                        itemsRef.child(itemId).removeValue().addOnCompleteListener(onCompleteListener);
                    });
                } else {
                    // No image to delete, just delete the item
                    itemsRef.child(itemId).removeValue().addOnCompleteListener(onCompleteListener);
                }
            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (onCompleteListener != null) {
                    TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
                    taskCompletionSource.setException(databaseError.toException()); // ✅ Properly sets the failure
                    onCompleteListener.onComplete(taskCompletionSource.getTask());
                }
            }

        });
    }
    
    // Upload image to Firebase Storage
    public static void uploadImage(Uri imageUri, String folder, final OnSuccessListener<Uri> onSuccessListener,
                                  final OnFailureListener onFailureListener) {
        if (imageUri == null) {
            onFailureListener.onFailure(new IllegalArgumentException("Image URI is null"));
            return;
        }
        
        String filename = UUID.randomUUID().toString();
        StorageReference fileRef = storageRef.child(folder + "/" + filename);
        
        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(onSuccessListener)
                    .addOnFailureListener(onFailureListener);
        }).addOnFailureListener(onFailureListener);
    }
} 