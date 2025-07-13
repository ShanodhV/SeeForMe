# Firebase Integration Setup Guide for See For Me App

## Prerequisites
1. Android Studio installed
2. Google account for Firebase Console access
3. See For Me project already set up

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter project name: `see-for-me-app` (or your preferred name)
4. Enable Google Analytics (recommended)
5. Choose or create a Google Analytics account
6. Click "Create project"

## Step 2: Add Android App to Firebase Project

1. In Firebase Console, click "Add app" and select Android
2. Enter your package name: `com.shanodh.seeforme`
3. Enter app nickname: `See For Me Android`
4. Get your SHA-1 fingerprint:
   ```bash
   # For debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   
   # For Windows
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Enter the SHA-1 fingerprint in Firebase Console
6. Click "Register app"

## Step 3: Download Configuration File

1. Download the `google-services.json` file
2. Replace the placeholder file at `app/google-services.json` with your downloaded file

## Step 4: Enable Firebase Services

### Authentication
1. In Firebase Console, go to "Authentication" > "Sign-in method"
2. Enable "Google" sign-in provider
3. Set up OAuth consent screen if prompted
4. Note down the Web client ID (you'll need this for Google Sign-In)

### Firestore Database
1. Go to "Firestore Database" > "Create database"
2. Choose "Start in test mode" for development
3. Select your preferred location
4. Click "Done"

### Storage
1. Go to "Storage" > "Get started"
2. Choose "Start in test mode"
3. Select your preferred location
4. Click "Done"

## Step 5: Update App Configuration

### Update GoogleAuthHelper.java
1. Open `app/src/main/java/com/shanodh/seeforme/auth/GoogleAuthHelper.java`
2. Replace `"YOUR_WEB_CLIENT_ID"` with your actual Web client ID from Firebase Console

### Firestore Security Rules (for development)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own data
    match /notes/{noteId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    
    match /familiar_faces/{faceId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
  }
}
```

### Storage Security Rules (for development)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /face_images/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Step 6: Build and Test

1. Sync your project in Android Studio
2. Build the project to ensure all dependencies are resolved
3. Run the app on a device or emulator
4. Test the Google sign-in functionality
5. Test adding notes and familiar faces
6. Verify data appears in Firebase Console

## Key Features Implemented

### Authentication
- Google Sign-In integration
- Automatic redirect to login if not authenticated
- Sign-out functionality in main menu

### Notes Management
- Save notes to both local database and Firebase Firestore
- Offline support with automatic sync when online
- User-specific data isolation

### Familiar Faces Management
- Save face images to Firebase Storage
- Store face metadata in Firestore
- Offline support with sync capability

### Data Synchronization
- Automatic sync of local data to cloud
- Download cloud data to local storage
- Conflict resolution based on timestamps

## Security Considerations

### For Production:
1. Update Firestore rules to be more restrictive
2. Update Storage rules to validate file types and sizes
3. Implement proper error handling
4. Add data validation on client and server side
5. Use proper authentication tokens
6. Implement rate limiting

### Firestore Production Rules Example:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /notes/{noteId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == resource.data.userId
        && validateNote(request.resource.data);
    }
    
    match /familiar_faces/{faceId} {
      allow read, write: if request.auth != null 
        && request.auth.uid == resource.data.userId
        && validateFace(request.resource.data);
    }
  }
  
  function validateNote(data) {
    return data.keys().hasAll(['content', 'timestamp', 'userId'])
      && data.content is string
      && data.content.size() <= 1000
      && data.timestamp is timestamp
      && data.userId is string;
  }
  
  function validateFace(data) {
    return data.keys().hasAll(['name', 'relationship', 'timestamp', 'userId'])
      && data.name is string
      && data.name.size() <= 100
      && data.relationship is string
      && data.relationship.size() <= 100
      && data.timestamp is timestamp
      && data.userId is string;
  }
}
```

## Troubleshooting

### Common Issues:
1. **Google Sign-In fails**: Check SHA-1 fingerprint and Web client ID
2. **Firestore permission denied**: Verify security rules and user authentication
3. **Storage upload fails**: Check storage rules and file permissions
4. **App crashes on auth**: Ensure google-services.json is properly configured

### Debug Steps:
1. Check Logcat for Firebase-related errors
2. Verify internet connectivity
3. Ensure Firebase services are enabled in console
4. Check if API keys are properly configured

## Next Steps

1. Implement background sync with WorkManager
2. Add image compression for face uploads
3. Implement data export/import functionality
4. Add multi-device sync notifications
5. Implement advanced accessibility features for cloud data

## Support

For issues related to:
- Firebase setup: Check Firebase documentation
- Android integration: Check Firebase Android guides
- App-specific issues: Review app logs and error messages
