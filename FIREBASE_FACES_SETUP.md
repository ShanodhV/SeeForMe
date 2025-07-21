# Firebase Setup for SeeForMe App

## Prerequisites
- Firebase project should already be created and `google-services.json` should be in the app folder
- Authentication should be enabled

## Required Firebase Configuration

### 1. Firestore Database Rules

In your Firebase Console, go to **Firestore Database → Rules** and update to:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow users to read/write their own face data
    match /faces/{faceId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
    
    // Allow users to read/write their own notes
    match /notes/{noteId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
    }
  }
}
```

### 2. Firestore Database Structure

The app will automatically create these collections:

#### faces Collection
```
faces/
  {faceId}/
    - id: string (unique face ID)
    - personName: string (person's name)
    - relationship: string (relationship to user)
    - localImagePath: string (path to local image file)
    - timestamp: number (creation timestamp)
    - userId: string (user's UID)
```

#### notes Collection  
```
notes/
  {noteId}/
    - id: string (unique note ID)
    - title: string (note title)
    - content: string (note content)
    - timestamp: number (creation timestamp)
    - userId: string (user's UID)
```

### 3. Authentication Setup

Make sure these authentication methods are enabled in Firebase Console:
- Email/Password authentication
- Google Sign-In (if using Google authentication)

### 4. Common Issues and Solutions

#### "Failed to save" or "Failed to load" errors:

1. **Check Authentication**: User must be logged in
2. **Check Rules**: Ensure Firestore rules are updated as shown above
3. **Check Network**: Device must have internet connection
4. **Check Logs**: Look for detailed error messages in Android logcat

#### Debug Steps:

1. Check logcat for detailed error messages:
   ```
   Tag: AddFaceActivity or ViewFacesActivity
   ```

2. Verify user is authenticated:
   ```
   Look for: "Loading faces for user: [USER_ID]"
   ```

3. Check if Firestore queries are working:
   ```
   Look for: "Query successful, found X documents"
   ```

### 5. Testing

1. **Add a face**: 
   - Fill in name and relationship
   - Take photo or select from gallery
   - Tap "Save Face"
   - Should see "Face saved successfully" message

2. **View faces**:
   - Go to View Faces screen
   - Should see loading spinner first
   - Should see saved faces or empty state

### 6. Troubleshooting

If faces aren't showing:
1. Check if user is logged in
2. Verify Firestore rules allow read access
3. Check if images exist in local storage
4. Look for error messages in logcat

If saving fails:
1. Check if user is logged in  
2. Verify Firestore rules allow write access
3. Check device storage permissions
4. Verify network connection
