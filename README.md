# Lost and Found App

A comprehensive platform for reporting and finding lost items, built with Android (Java) and Firebase, with a Laravel admin panel.

![Lost and Found App](https://via.placeholder.com/800x400?text=Lost+and+Found+App)

## Features

### Android App
- User Authentication (Email/Password via Firebase)
- Post Lost/Found Items with Images
- Browse Items with Filtering
- Contact Poster via Call/SMS
- User Profile with Post History
- Beautiful Material Design UI


## Project Structure

### Android App
```
app/
├─ src/
│  ├─ main/
│  │  ├─ java/com.example.lostandfound/
│  │  │  ├─ activities/
│  │  │  │  ├─ LoginActivity.java
│  │  │  │  ├─ SignUpActivity.java
│  │  │  │  ├─ PostItemActivity.java
│  │  │  │  ├─ ItemDetailActivity.java
│  │  │  │  ├─ ProfileActivity.java
│  │  │  │  ├─ SplashActivity.java
│  │  │  ├─ adapters/
│  │  │  │  ├─ ItemAdapter.java
│  │  │  ├─ models/
│  │  │  │  ├─ Item.java
│  │  │  │  ├─ User.java
│  │  │  ├─ utils/
│  │  │  │  ├─ FirebaseUtils.java
│  │  │  ├─ MainActivity.java
│  ├─ res/
│  │  ├─ layout/
│  │  ├─ drawable/
│  │  ├─ values/
│  │  ├─ anim/
```


## Getting Started

### Android App Setup

1. Clone this repository
2. Open the project in Android Studio
3. Follow the [Firebase setup guide](FIREBASE_SETUP.md)
4. Build and run the app



## Firebase Integration

The app uses Firebase for:
- Authentication (email/password)
- FireStore Databse (storing items and user data)
- Realtime Database (storing items and user data)
- Storage (for images)

## Screenshots

![Login Screen](https://via.placeholder.com/200x400?text=Login+Screen)
![Main Screen](https://via.placeholder.com/200x400?text=Main+Screen)
![Post Item](https://via.placeholder.com/200x400?text=Post+Item)
![Item Details](https://via.placeholder.com/200x400?text=Item+Details)
![Admin Dashboard](https://via.placeholder.com/600x300?text=Admin+Dashboard)

## Implementation Details

### Android App

#### User Authentication
- Secure login and registration with Firebase Auth
- Profile management
- Password recovery

#### Item Management
- Post lost or found items with detailed information
- Upload images from camera or gallery
- Browse items with filtering options
- View detailed item information

#### UI/UX
- Material Design components
- Smooth animations and transitions
- Pull-to-refresh for latest content
- Bottom navigation for easy access



### Android App
- Generate signed APK using Android Studio
- Upload to Google Play Store



## Future Enhancements

- Chat feature for direct communication
- Location-based search
- Push notifications
- Social media sharing
- Multi-language support
- Dark mode

## Credits

Developed by [Sengchheav]

## License

This project is licensed under the MIT License - see the LICENSE file for details. 