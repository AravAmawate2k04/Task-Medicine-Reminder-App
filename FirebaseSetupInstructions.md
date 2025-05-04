# Firebase Setup Instructions for Task and Medicine Reminder App

Before testing your Firestore functionality, make sure you have completed the following steps in the Firebase Console:

## 1. Enable Firestore Database

1. Go to your [Firebase Console](https://console.firebase.google.com/)
2. Select your project: "task-and-medicine-reminder-app"
3. In the left sidebar, click on "Firestore Database"
4. Click "Create Database" if you haven't created one yet
5. Choose a starting mode for your security rules:
   - **Test mode** for development (allows all reads and writes)
   - **Production mode** for stricter security

## 2. Set up Firestore Security Rules

You need to configure security rules to allow authenticated users to access only their own data.

1. Go to the "Firestore Database" section
2. Click on the "Rules" tab
3. Replace the default rules with the following:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Make sure the user is authenticated
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Allow access to subcollections for the authenticated user
      match /tasks/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /medicines/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /wellness/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

4. Click "Publish"

## 3. Verify Authentication Settings

1. Go to "Authentication" in the left sidebar
2. Make sure Email/Password authentication is enabled
3. Verify that you have successfully created users that you can log in with

## 4. Test Your App

1. Log in to your app using valid credentials
2. Go to the Dashboard screen
3. Click the "Test Firestore Data Storage" button
4. Check Logcat in Android Studio with the filter tag "FirestoreTestUtil" to view the operation results

## 5. View Data in Firebase Console

After testing the app:

1. Go to the "Firestore Database" section in the Firebase Console
2. Click on the "Data" tab
3. You should see a "users" collection with a document ID matching your user's UID
4. Inside that document, you should see subcollections for:
   - tasks
   - medicines
   - wellness (with sleepLogs and activityLogs subcollections)

## Troubleshooting

If data is not appearing in Firestore:

1. Check Logcat for error messages
2. Verify that you are logged in (you need to be authenticated to access Firestore)
3. Confirm your security rules allow the operation
4. Make sure your device/emulator has internet connectivity
5. Verify that the Firebase project in your app matches the one in the console 