# SeeForMe - AI-Powered Visual Assistant

An Android application designed to assist visually impaired individuals with AI-powered features including face recognition, voice commands, and smart navigation assistance.

## Features

### 🎯 Core Features
- **Voice Assistant**: Hands-free interaction with voice commands
- **Face Recognition**: Familiar faces detection and management
- **Smart Notes**: Voice-to-text note taking with cloud sync
- **Firebase Authentication**: Secure Google Sign-In integration
- **Accessibility First**: Designed with accessibility principles

### 🎨 Modern UI
- **Glassmorphism Design**: Beautiful modern interface with glass effects
- **Material Design 3**: Latest Material Design components
- **High Contrast**: Accessibility-friendly color schemes
- **Voice Feedback**: TTS (Text-to-Speech) for all interactions

### 🔧 Technical Features
- **Firebase Integration**: Authentication and Firestore database
- **Room Database**: Local data persistence
- **Voice Recognition**: Advanced speech-to-text capabilities
- **Camera Integration**: Photo capture for face recognition
- **Cloud Sync**: Real-time data synchronization

## Screenshots

### Login & Registration
- Modern glassmorphism login interface
- Google Sign-In integration
- Secure account creation

### Main Interface
- Voice-activated assistant
- Accessible navigation
- Real-time status updates

### Face Recognition
- Add familiar faces
- Smart recognition system
- Voice announcements

## Technologies Used

- **Language**: Java
- **Platform**: Android (API 24+)
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (local) + Firestore (cloud)
- **Authentication**: Firebase Auth with Google Sign-In
- **UI Framework**: Material Design 3
- **Voice Services**: Android Speech Recognition & TTS
- **Image Processing**: Camera2 API with face detection

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24 or higher
- Google account for Firebase services

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/ShanodhV/SeeForMe.git
   ```

2. Open the project in Android Studio

3. Configure Firebase:
   - Add your `google-services.json` file to the `app` directory
   - Follow the [Firebase Setup Guide](FIREBASE_SETUP.md)

4. Build and run the application

### Firebase Configuration
1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Enable Authentication with Google Sign-In
3. Set up Firestore database
4. Download and add `google-services.json` to your app

## Project Structure

```
app/
├── src/main/java/com/shanodh/seeforme/
│   ├── auth/           # Authentication managers
│   ├── data/           # Room database entities
│   ├── firebase/       # Firebase services
│   ├── models/         # Data models
│   ├── ui/            # Activities and fragments
│   │   ├── fragments/ # Main app fragments
│   │   └── components/ # Custom UI components
│   ├── voice/         # Voice recognition and TTS
│   ├── utils/         # Utility classes
│   └── adapters/      # RecyclerView adapters
├── res/
│   ├── layout/        # XML layouts
│   ├── drawable/      # Vector graphics and backgrounds
│   ├── values/        # Colors, strings, styles
│   └── navigation/    # Navigation components
└── AndroidManifest.xml
```

## Features in Development

- [ ] Advanced face recognition with ML Kit
- [ ] Smart object detection
- [ ] GPS navigation assistance
- [ ] Wearable device integration
- [ ] Multi-language support
- [ ] Offline mode capabilities

## Accessibility Features

- **Voice Navigation**: Complete app navigation through voice commands
- **High Contrast UI**: Optimized for low vision users
- **Large Touch Targets**: Easy interaction for motor impairments
- **Screen Reader Support**: Full TalkBack compatibility
- **Haptic Feedback**: Tactile confirmation for actions

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

**Developer**: ShanodhV  
**Email**: shanodh6262@gmail.com  
**GitHub**: [@ShanodhV](https://github.com/ShanodhV)

## Acknowledgments

- Firebase for backend services
- Material Design team for UI guidelines
- Android accessibility team for accessibility APIs
- Open source community for various libraries used

---

**SeeForMe** - Empowering independence through technology 🌟
