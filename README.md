# NEW UNIKIDS APP

Bilingual Android learning app for students and teachers, built with Kotlin and Jetpack Compose.

## App capabilities

- Sinhala / English language selection, remembered on the device
- Student and Teacher login using mobile number and password
- Student and Teacher registration with address and profile photo
- Student registration with DOB, school, grade and multiple active class selection
- Registration confirmation with role- and language-specific welcome voice
- Student and Teacher profiles
- Class-specific schedules, Zoom links, video lessons and study materials
- Teacher submission forms with Admin approval workflow
- Student payment submissions, payment history and installment records
- Student and Teacher notifications
- Student learning-resource overview
- UNIKIDS logo, 3D mascots and learning icons
- White UI backgrounds with the UNIKIDS purple, yellow, red and blue accent palette

## Technology

- Kotlin / Jetpack Compose
- Google Apps Script API
- Google Sheets database
- Google Drive media links
- GitHub Actions Android APK builds
- No Firebase and no paid backend services

## Android package

`com.unikids.app`

## Developer credit

Powered by SmartSolution PVT Ltd  
Developed by Menaka Nuwan kumarasinghe

## Build

GitHub Actions builds an installable Debug APK on every push to `main`.

Workflow artifact name: `NEW-UNIKIDS-APP-APK`  
APK output: `app/build/outputs/apk/debug/app-debug.apk`
