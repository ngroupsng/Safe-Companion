# Safe Companion
### Personal Safety & Journey Monitoring System

Safe Companion is an Android-based personal safety and journey monitoring application designed to improve travel safety, provide emergency assistance, and create awareness of potential hazards along a journey.

The project focuses on two major concerns: emergency assistance when internet connectivity is limited and awareness of potentially unsafe routes while travelling.

---

## Problem Statement

Existing personal safety systems often depend on internet connectivity for location tracking, monitoring, and emergency communication.

Additionally, navigation applications primarily focus on finding routes and reaching destinations, but users may still encounter unfamiliar or potentially unsafe roads.

Safe Companion aims to address these concerns through offline-oriented safety features, journey monitoring, map-based hazard awareness, and community support.

## Key Features

### 1. SOS Emergency Assistance
- Emergency alert functionality.
- Location-based emergency information.
- Sharing geographical coordinates (latitude and longitude).

### 2. Offline SOS Support
- Supports offline-oriented emergency functionality.
- Uses geographical coordinates to provide location information.
- Reduces dependence on continuous internet connectivity for supported local operations.

### 3. Map-Based Hazard Awareness
- Displays potential hazardous areas on the map.
- Helps users become more aware of route conditions.
- Supports better-informed travel decisions.

### 4. Journey Monitoring
- Tracks journey progress.
- Processes location updates.
- Evaluates journey and safety-related conditions.

### 5. Community Support
- Integrates community assistance into the personal safety system.
- Extends the safety ecosystem beyond individual emergency alerts.

### 6. Offline Monitoring
- Uses locally available data for supported monitoring functionality.
- Maintains supported local records when internet connectivity is unavailable.

---

## System Architecture

The application follows a modular architecture that separates the user interface, application state, location tracking, journey management, local persistence, map presentation, and emergency communication.

### Architecture Components

| Component | Responsibility |
|---|---|
| Jetpack Compose UI | User interface and user interactions |
| ViewModel | Application state and safety conditions |
| Location Tracking | Acquires and processes location updates |
| Journey Management | Manages journey progress |
| Room Database | Stores supported local records |
| Map Integration | Displays locations and hazard information |
| Emergency Communication | Handles SOS-related communication |

### Architecture Flow

```text
          User
           |
           v
    Jetpack Compose UI
           |
           v
        ViewModel
           |
    +------+------+ 
    |      |      |
    v      v      v
 Location Journey Safety
 Tracking Management Logic
    |      |      |
    +------+------+
           |
     +-----+------+
     |            |
     v            v
 Room Database  Map Integration
     |
     v
 Local Data

       ViewModel
           |
           v
 Emergency Communication
           |
           v
      SOS Assistance
```

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | UI development |
| Android ViewModel | State management |
| Room Database | Local data persistence |
| GPS / Location Services | Location acquisition |
| Map Integration | Location and hazard visualization |
| Android SDK | Application platform |

---

## Future Enhancements

Safe Companion is a completed initial project version, but development can continue with several improvements.

- **AI Integration:** Introduce AI-based capabilities for more intelligent journey monitoring and safety-related risk analysis.
- **Improved GPS Accuracy:** Work towards approximately 5-metre location accuracy, depending on device and environmental conditions.
- **Intelligent Hazard Detection:** Explore more advanced methods of identifying potentially hazardous routes.
- **Enhanced Journey Monitoring:** Improve tracking reliability and safety-related monitoring.
- **Extended Community Support:** Explore additional ways to strengthen community-based assistance.

---

## Project Motivation

The idea behind Safe Companion came from real-life observations and experiences.

Growing up in a family where the safety of girls is a constant concern raised questions about safe destinations, routes, and travelling alone.

A personal experience involving an unfamiliar route with narrow lanes further highlighted the need for better awareness of road conditions.

These observations inspired the development of Safe Companion — a project focused on making journeys more safety-conscious.

---

## Project Status

**Current Version:** Initial project completed.

**Development Status:** Future enhancements planned.

---

## Contributors

Developed as a collaborative project with a focus on personal safety, journey monitoring, and technology-driven solutions.

---

### Safe Companion

*It's not just about reaching a destination. It's about making the journey more safety-conscious.*
