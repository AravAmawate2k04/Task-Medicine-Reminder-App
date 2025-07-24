# Task & Medicine Reminder App

A comprehensive mobile application designed to streamline the management of daily tasks, medicine schedules, wellness tracking, and productivity for users. This project integrates the best practices in time management, healthcare reminders, and personal productivity, providing a unified solution for individuals seeking to optimize their routines and well-being.

---

## Key Features

### 1. **Task Management with Priority Quadrants**
- **Eisenhower Matrix-Based Prioritization:**  
  Tasks are categorized into four quadrants:
  - Urgent & Important
  - Urgent, Not Important
  - Not Urgent, Important
  - Not Urgent, Not Important  
  This helps users focus on what truly matters and manage their time efficiently.
- **Recurring Tasks:**  
  Support for weekly and custom recurring tasks to automate routine scheduling.
- **Deadline & Reminder System:**  
  Set deadlines and receive timely reminders before tasks are due, reducing missed deadlines and last-minute rush.

### 2. **Medicine Scheduling and Reminders**
- **Flexible Medicine Timetables:**  
  Add medicines with custom times, durations, and food instructions.
- **Active/Inactive Tracking:**  
  Track which medicines are currently active and maintain a taken history for compliance.
- **Multiple Daily Reminders:**  
  Support for medicines that need to be taken multiple times per day.

### 3. **Wellness Tracking**
- **Sleep Logging:**  
  Record sleep start and wake times, interruptions, and quality ratings.  
  Analyze patterns with good, interrupted, and poor sleep logs.
- **Activity Monitoring:**  
  Log daily steps and physical activity, set daily goals, and monitor progress.

### 4. **Intuitive and Visual Dashboard**
- **Productivity Insights:**  
  Visual representation of task priorities and completion status.
- **Health Overview:**  
  Quick glance at today's medicines, sleep, and activity logs for holistic well-being management.

### 5. **Seamless Firebase Integration**
- **Cloud Firestore Database:**  
  Real-time sync and secure storage for all user data including tasks, medicines, sleep, and activity logs.
- **User Authentication:**  
  Secure sign-in and data isolation for each user.

---

## Tech Stack

- **Kotlin** & **Jetpack Compose**: Modern, declarative UI and robust Android development.
- **Firebase** (Firestore, Authentication): Real-time NoSQL database and secure user authentication.
- **Coroutines**: For asynchronous programming and responsive UI.
- **Material Design**: Ensures a modern, accessible, and intuitive look and feel.
- **MVVM Architecture**: Clean separation of concerns for scalable, maintainable code.

---

## Requirements

- **Android Studio Flamingo (or newer)** recommended
- **Android Device or Emulator** running API 23 (Android 6.0) or above
- **Google Firebase Account**
- **Internet Connectivity** for Firebase syncing

**Dependencies:**  
Refer to the project's `build.gradle` files for the complete list of required library dependencies.

---

## Professional Impacts

- **Improved Productivity:**  
  By adopting the Eisenhower Matrix, users learn to delegate, schedule, and focus, resulting in better task completion and less stress.
- **Better Health Compliance:**  
  Automated medicine reminders ensure timely intake, supporting chronic care and general wellness.
- **Personalized Self-Management:**  
  The unified platform empowers users to take control of their routines and health, backed by data-driven insights.
- **Scalable & Secure:**  
  Built on Firebase, the app is ready for future growth and upholds high standards for data privacy and security.

---

## Getting Started

1. **Clone the Repository**
2. **Follow [Firebase Setup Instructions](FirebaseSetupInstructions.md)** for backend configuration and security.
3. **Run the App** on your preferred Android device or emulator.

---

## Troubleshooting

- Ensure you are signed in and your device has internet connectivity.
- Check Firestore security rules if data is not syncing.
- Refer to Logcat for debugging authentication or data access issues.

---

## Contribution

We welcome contributions that improve usability, add features, or enhance code quality. Please submit a pull request or open an issue to discuss your ideas.

---

## License

[MIT License](LICENSE)

---

> **Empowering users to live healthier, more productive lives—one reminder at a time.**
