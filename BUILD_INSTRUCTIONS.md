# Build Instructions

This document provides detailed instructions for building the Custom Vibe Watch Faces Android application.

## Prerequisites

### Required Software
- **JDK**: Java Development Kit 17 or higher
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Android SDK**: API Level 26 (Android 8.0) minimum, API Level 34 (Android 14) target
- **Gradle**: 8.2 (included via wrapper)

### Required SDK Components
Install these via Android Studio SDK Manager:
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Google Play services
- Android Support Repository
- Google Repository

## Building the Project

### Option 1: Using Android Studio (Recommended)

1. **Open the Project**:
   ```
   File → Open → Select the project root directory
   ```

2. **Sync Project**:
   - Android Studio will automatically start syncing Gradle
   - Wait for the sync to complete (may take a few minutes on first run)
   - Resolve any SDK-related prompts by clicking "Install missing components"

3. **Build the Mobile App**:
   ```
   Build → Make Project
   ```
   Or use the menu:
   ```
   Build → Select Build Variant → Select 'mobile' → Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Build the Wear App**:
   ```
   Build → Select Build Variant → Select 'wear' → Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

### Option 2: Using Command Line

1. **Navigate to Project Directory**:
   ```bash
   cd custom-vibe-watch-faces
   ```

2. **Make gradlew Executable** (Linux/Mac):
   ```bash
   chmod +x gradlew
   ```

3. **Build Debug APKs**:
   ```bash
   # Build mobile app
   ./gradlew :mobile:assembleDebug
   
   # Build wear app
   ./gradlew :wear:assembleDebug
   
   # Build both
   ./gradlew assembleDebug
   ```

4. **Build Release APKs** (requires signing configuration):
   ```bash
   ./gradlew assembleRelease
   ```

5. **Clean Build**:
   ```bash
   ./gradlew clean
   ```

## Output Locations

After building, find the APK files at:

- **Mobile App**: `mobile/build/outputs/apk/debug/mobile-debug.apk`
- **Wear App**: `wear/build/outputs/apk/debug/wear-debug.apk`

## Installing the Apps

### Installing on Physical Devices

#### Mobile App
```bash
adb install mobile/build/outputs/apk/debug/mobile-debug.apk
```

#### Wear App
1. Enable ADB debugging on your WearOS watch:
   - Go to Settings → About → Build number (tap 7 times)
   - Go to Developer options → Enable ADB debugging
   
2. Connect watch via Bluetooth debugging or USB (if supported):
   ```bash
   # List connected devices
   adb devices
   
   # Install on specific device
   adb -s <device-id> install wear/build/outputs/apk/debug/wear-debug.apk
   ```

### Installing via Android Studio

1. Connect your device(s)
2. Select the run configuration (mobile or wear)
3. Click the Run button (green play icon)
4. Select your target device from the list

## Troubleshooting Build Issues

### "SDK location not found"
Create a `local.properties` file in the project root:
```properties
sdk.dir=/path/to/your/Android/Sdk
```

### "Failed to resolve: com.android.tools.build:gradle"
Ensure you have internet access and can reach:
- https://dl.google.com
- https://repo1.maven.org

### "Unsupported class file major version"
Your JDK version is too new or too old. Use JDK 17.

### "Execution failed for task ':mobile:compileDebugKotlin'"
1. File → Invalidate Caches / Restart
2. Clean and rebuild:
   ```bash
   ./gradlew clean build
   ```

### Compose Compiler Issues
If you see Compose-related errors, verify in `mobile/build.gradle.kts`:
- `kotlinCompilerExtensionVersion` matches your Kotlin version
- Compose BOM version is compatible

## Development Setup

### Running Lint
```bash
./gradlew lint
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :mobile:test
./gradlew :wear:test
```

### Code Style
The project follows standard Kotlin coding conventions. Format code with:
- Android Studio: `Ctrl+Alt+L` (Windows/Linux) or `Cmd+Option+L` (Mac)
- Command line: `./gradlew ktlintFormat` (if ktlint is configured)

## Signing Release Builds

For production releases, you need to sign your APKs:

1. **Create a keystore**:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```

2. **Add signing config** to `mobile/build.gradle.kts` and `wear/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("path/to/your/keystore.jks")
               storePassword = "your-store-password"
               keyAlias = "your-key-alias"
               keyPassword = "your-key-password"
           }
       }
       
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ... other config
           }
       }
   }
   ```

3. **Build signed APK**:
   ```bash
   ./gradlew assembleRelease
   ```

**Security Note**: Never commit keystores or passwords to version control. Use environment variables or a secure configuration file that's in `.gitignore`.

## Continuous Integration

### GitHub Actions Example

```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Run tests
      run: ./gradlew test
```

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/)
- [WearOS Developer Guide](https://developer.android.com/training/wearables)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

## Support

If you encounter build issues not covered here:
1. Check the GitHub Issues page
2. Review the Android Studio build output for specific error messages
3. Ensure all prerequisites are correctly installed
4. Try cleaning and rebuilding the project

For project-specific questions, please open an issue on the GitHub repository.
