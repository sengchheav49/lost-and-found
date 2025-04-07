package com.example.lostandfound.models;

import android.widget.ImageView;
import android.widget.TextView;

import java.time.Instant;
import java.util.Date;

public class Item {
    private String itemId;
    private String title;
    private String description;
    private String category;
    private String location;
    private Date date;
    private Date returnDate;
    private String itemType; // "lost" or "found"
    private String imageUrl;
    private String userId;
    private String userName;
    private String userPhoneNumber;
    private long timestamp;

    // Required empty constructor for Firebase
    public Item() {
        this.timestamp = new Date().getTime();
    }

    public Item(String itemId, String title, String description, String category, 
                String location, Date date,Date returnDate, String itemType, String imageUrl,
                String userId, String userName, String userPhoneNumber) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.returnDate = returnDate;
        this.date = date;
        this.itemType = itemType;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.userName = userName;
        this.userPhoneNumber = userPhoneNumber;
        this.timestamp = new Date().getTime();
    }




    public Item(String itemId, String title, String description, String category, String location, Date date,Date returnDate, String itemType, String imageUrl, String userId, String contact) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.date = date;
        this.returnDate = returnDate;
        this.itemType = itemType;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.userName = userName;
        this.userPhoneNumber = contact;
        this.timestamp = new Date().getTime();
    }


    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }
    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhoneNumber() {
        return userPhoneNumber;
    }

    public void setUserPhoneNumber(String userPhoneNumber) {
        this.userPhoneNumber = userPhoneNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 