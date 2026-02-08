# 🛡️ TerraGuard

<div align="center">

![TerraGuard Logo](https://img.shields.io/badge/TerraGuard-Emergency%20Preparedness-4CAF50?style=for-the-badge&logo=shield&logoColor=white)

**Your Personal Safety & Emergency Preparedness Companion**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Mapbox](https://img.shields.io/badge/Maps-Mapbox-000000?style=flat-square&logo=mapbox&logoColor=white)](https://www.mapbox.com/)

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Tech Stack](#-tech-stack) • [API Setup](#-api-setup) • [Contributing](#-contributing)

</div>

---

## 📖 About

**TerraGuard** is a comprehensive Android application designed to enhance personal safety and emergency preparedness. It provides real-time environmental monitoring, emergency contact management, and location-based services to help users stay informed and prepared for any situation.

Whether you're tracking air quality, monitoring weather patterns, or need quick access to emergency contacts, TerraGuard has you covered.

---

## ✨ Features

### 🌡️ Real-Time Weather Monitoring
- **Current Temperature** - Live temperature data based on your location
- **24-Hour Temperature Trend** - Interactive line chart showing temperature forecast
- **Weather Conditions** - Current weather status (Sunny, Cloudy, Rainy, etc.)

### 🌬️ Air Quality Index (AQI)
- **Real-time AQI Data** - Monitor air quality levels in your area
- **Color-Coded Status** - Visual indicators (Good, Fair, Moderate, Poor, Very Poor)
- **Health Recommendations** - Stay informed about outdoor activity safety

### 🗺️ Interactive Map
- **Mapbox Integration** - Beautiful, responsive maps
- **Location Tracking** - Real-time GPS location display
- **Weather Overlay** - See weather and AQI data directly on the map
- **Saved Locations** - Mark and save important locations

### 📞 Emergency Contacts
- **Quick Dial** - One-tap calling for emergency contacts
- **Predefined Numbers** - Police, Fire Department, Ambulance
- **Custom Contacts** - Add family, friends, and other important contacts
- **Contact Import** - Import contacts from your phone's contact list
- **Call Log Access** - View recent calls for quick re-dial

### 👤 User Profile & Preparedness
- **Safety Score** - Track your emergency preparedness level
- **Preparedness Checklist** - Emergency kit, offline maps, emergency plan
- **Medical Information** - Store blood type, allergies (shared only in SOS mode)
- **Achievements** - Gamified badges for completing safety tasks
- **Statistics** - View drills completed, alerts received

### 🔐 Secure Authentication
- **Google Sign-In** - Quick and secure authentication via Firebase
- **Email/Password Login** - Traditional login option
- **Account Management** - Easy sign-out and profile management

### 🚨 Emergency Features
- **Flashlight Control** - Quick toggle for emergency flashlight
- **Volume/Sound Control** - Emergency sound alerts
- **Vibration Alerts** - Haptic feedback for notifications

---

## 📱 Screenshots

| Home Dashboard | Profile | Map View | Emergency Contacts |
|:---:|:---:|:---:|:---:|
| Weather & AQI | Safety Score | Location | Quick Dial |

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java |
| **Platform** | Android (Min SDK 24, Target SDK 36) |
| **UI Components** | Material Design 3, ConstraintLayout |
| **Authentication** | Firebase Auth, Google Sign-In |
| **Maps** | Mapbox SDK |
| **Weather API** | OpenWeatherMap API |
| **Charts** | MPAndroidChart |
| **Networking** | Retrofit 2, Gson |
| **Location** | Google Play Services Location |
| **Database** | SQLite (Local storage) |

---

## 📦 Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Java 8+
- Git

### Clone the Repository
```bash
git clone https://github.com/animeshD06/TeraGaurd.git
cd TeraGaurd
```

### Setup API Keys

1. **OpenWeatherMap API Key**
   - Sign up at [OpenWeatherMap](https://openweathermap.org/api)
   - Get your free API key
   - Add it to `MainActivity.java`:
   ```java
   private static final String API_KEY = "YOUR_API_KEY";
   ```

2. **Mapbox Access Token**
   - Sign up at [Mapbox](https://www.mapbox.com/)
   - Get your public access token
   - Add it to `res/values/strings.xml`:
   ```xml
   <string name="mapbox_access_token">YOUR_MAPBOX_TOKEN</string>
   ```

3. **Firebase Configuration**
   - Create a project in [Firebase Console](https://console.firebase.google.com/)
   - Enable Google Sign-In authentication
   - Download `google-services.json`
   - Place it in the `app/` directory

4. **Mapbox Downloads Token** (for SDK download)
   - Add to `gradle.properties`:
   ```properties
   MAPBOX_DOWNLOADS_TOKEN=YOUR_SECRET_TOKEN
   ```

### Build & Run
```bash
# Open in Android Studio
# Sync Gradle files
# Build and run on device/emulator
```

---

## 🔑 API Setup

### OpenWeatherMap (Free Tier)
The app uses these endpoints:
- `/weather` - Current weather data
- `/air_pollution` - Air Quality Index
- `/forecast` - 5-day/3-hour forecast

### Mapbox
- Map display with custom styling
- Location tracking and markers

### Firebase
- Authentication (Google Sign-In)
- User session management

---

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/teragaurd/
│   │   ├── MainActivity.java           # Home dashboard
│   │   ├── LoginActivity.java          # Authentication
│   │   ├── MapActivity.java            # Map view
│   │   ├── EmergencyContactsActivity.java  # Contacts management
│   │   ├── profile_view_Activity.java  # User profile
│   │   ├── ApiService.java             # Retrofit API interface
│   │   ├── WeatherResponse.java        # Weather data model
│   │   ├── AqiResponse.java            # AQI data model
│   │   ├── ForecastResponse.java       # Forecast data model
│   │   ├── EmergencyContact.java       # Contact model
│   │   ├── ContactDAO.java             # Database operations
│   │   ├── DatabaseHelper.java         # SQLite helper
│   │   ├── CallLogHelper.java          # Call log access
│   │   └── EmergencySettingsHelper.java # System settings
│   │
│   └── res/
│       ├── layout/                      # XML layouts
│       ├── drawable/                    # Icons & backgrounds
│       ├── values/                      # Strings, colors, themes
│       └── menu/                        # Navigation menus
```

---

## 🎨 Design Philosophy

TerraGuard follows modern Android design principles:

- **Dark Theme** - Eye-friendly dark UI with accent colors
- **Material Design 3** - Latest Material components
- **Flat ConstraintLayout** - Optimized view hierarchy
- **Color-Coded Information** - Intuitive status indicators
- **Smooth Animations** - Chart animations, transitions
- **Accessibility** - Content descriptions, readable text sizes

---

## 🔒 Permissions

| Permission | Usage |
|------------|-------|
| `INTERNET` | API calls, authentication |
| `ACCESS_FINE_LOCATION` | GPS for weather & map |
| `ACCESS_COARSE_LOCATION` | Approximate location |
| `CALL_PHONE` | Emergency dialing |
| `READ_CONTACTS` | Import contacts |
| `READ_CALL_LOG` | Recent calls display |
| `CAMERA` | Flashlight control |
| `VIBRATE` | Alert notifications |

---

## 🚀 Future Roadmap

- [ ] **Offline Mode** - Cached weather data
- [ ] **Push Notifications** - Weather alerts
- [ ] **SOS Mode** - One-tap emergency broadcast
- [ ] **Family Sharing** - Share location with family
- [ ] **Disaster Alerts** - Government alert integration
- [ ] **First Aid Guide** - In-app emergency instructions
- [ ] **Widget Support** - Home screen weather widget

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Animesh Dubey**

[![GitHub](https://img.shields.io/badge/GitHub-animeshD06-181717?style=flat-square&logo=github)](https://github.com/animeshD06)

---

## 🙏 Acknowledgments

- [OpenWeatherMap](https://openweathermap.org/) - Weather & AQI data
- [Mapbox](https://www.mapbox.com/) - Map services
- [Firebase](https://firebase.google.com/) - Authentication
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) - Chart library
- [Material Design](https://material.io/) - UI guidelines

---

<div align="center">

**Made with ❤️ for a safer world**

⭐ Star this repo if you find it useful!

</div>
