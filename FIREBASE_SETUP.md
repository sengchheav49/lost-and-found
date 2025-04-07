# Firebase Setup Guide for Lost and Found App

This guide walks you through setting up Firebase for your Lost and Found Android app.

## Prerequisites
- Android Studio
- A Google account
- The Lost and Found app codebase

## Step 1: Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter "Lost and Found" as the project name
4. Accept the terms and conditions
5. Click "Continue"
6. (Optional) Disable Google Analytics if not needed
7. Click "Create project"
8. Wait for project creation to complete
9. Click "Continue"

## Step 2: Register Your Android App with Firebase

1. In the Firebase console, click the Android icon (</>) to add an Android app
2. Enter your app's package name: `com.example.lostandfound`
3. Enter "Lost and Found" as the app nickname
4. Leave the Debug signing certificate SHA-1 blank (optional for now)
5. Click "Register app"

## Step 3: Download and Add the Configuration File

1. Download the `google-services.json` file
2. Place this file in your app module directory (`app/`)

## Step 4: Enable Authentication Services

1. In the Firebase console, click "Authentication" in the left sidebar
2. Click "Get started"
3. Select the "Email/Password" provider
4. Toggle the "Enable" switch to on
5. Click "Save"

## Step 5: Set Up Realtime Database

1. In the Firebase console, click "Realtime Database" in the left sidebar
2. Click "Create database"
3. Choose a location (usually the one closest to your users)
4. Start in test mode (we'll update security rules later)
5. Click "Enable"

## Step 6: Configure Storage Rules

1. In the Firebase console, click "Storage" in the left sidebar
2. Click "Get started"
3. Accept the default storage rules for now
4. Click "Next"
5. Choose a location (the same as your database location)
6. Click "Done"

## Step 7: Update Security Rules (Important for Production)

For the Realtime Database, update the rules to:

```json
{
  "rules": {
    "Users": {
      "$uid": {
        ".read": "auth !== null",
        ".write": "$uid === auth.uid"
      }
    },
    "Items": {
      ".read": "auth !== null",
      "$itemId": {
        ".write": "auth !== null"
      }
    }
  }
}
```

For Storage, update the rules to:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /item_images/{imageId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    match /profile_images/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Final Notes

- Keep the `google-services.json` file secure and do not commit it to public repositories
- For production, consider tightening security rules further
- Set up Firebase Authentication backup and recovery options 