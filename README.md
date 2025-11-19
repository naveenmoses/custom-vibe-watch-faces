# Custom Vibe Watch Faces

An Android companion app for WearOS smartwatches that generates custom watch faces using AI. Users can create both analog and digital watch faces with customizable backgrounds, fonts, colors, and styles using natural language prompts.

## Features

- 🎨 **AI-Powered Generation**: Uses Google Gemini AI to interpret your design prompts
- ⌚ **Analog & Digital**: Create both analog and digital watch faces
- 🎭 **Customizable Styles**:
  - **Digital**: Bold, Light, Mono, or Serif fonts
  - **Analog**: Classic, Modern, Sword, Minimal, or Ornate dial hands
  - **Backgrounds**: Gradient, Radial, Nature, Metallic, or Abstract themes
- 🌈 **Color Customization**: AI-selected color schemes based on your prompt
- 📱 **Real-time Sync**: Instantly sync generated watch faces to your WearOS device
- 🆓 **Free AI Service**: Uses Google Gemini's free tier API

## Example Prompts

### Digital Watch Faces
- "Create a digital watch face with nature background and bold white digits"
- "Digital watch with abstract colorful background, light mono font, and cyan color"
- "Modern digital watch with metallic silver background and serif numbers"

### Analog Watch Faces
- "Create an analog watch face with metallic background, sword-style hands, no numbers"
- "Analog watch with ocean blue gradient, classic white hands, and elegant serif numbers"
- "Minimalist analog watch with abstract background, thin modern hands, hour markers only"

## Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 26 (Android 8.0) or higher
- WearOS device or emulator

### Getting a Gemini API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated key
5. Open the app and go to Settings (gear icon)
6. Paste your API key and save

The Gemini API has a generous free tier suitable for personal use.

## Installation

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/naveenmoses/custom-vibe-watch-faces.git
   cd custom-vibe-watch-faces
   ```

2. Open the project in Android Studio

3. Build the mobile app module:
   ```bash
   ./gradlew :mobile:assembleDebug
   ```

4. Build the wear module:
   ```bash
   ./gradlew :wear:assembleDebug
   ```

5. Install on your devices:
   - Install mobile APK on your phone
   - Install wear APK on your WearOS watch

## Usage

1. **Set up API Key**:
   - Open the app on your phone
   - Tap the Settings icon (gear)
   - Enter your Gemini API key
   - Tap "Save API Key"

2. **Generate a Watch Face**:
   - Return to the main screen
   - Enter a descriptive prompt for your desired watch face
   - Tap "Generate Watch Face"
   - Wait for the AI to generate your design (usually 5-10 seconds)

3. **Preview**:
   - Review the generated watch face preview
   - If you're not satisfied, modify your prompt and try again

4. **Sync to Watch**:
   - Tap "Sync to Watch"
   - The watch face will automatically update on your WearOS device
   - Select the "Custom Vibe" watch face from your watch's face picker

## Architecture

### Mobile App (`mobile` module)
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel and Repository pattern
- **API Integration**: Retrofit for Gemini API calls
- **Image Generation**: Custom Canvas-based rendering with AI-guided parameters

### WearOS App (`wear` module)
- **Watch Face**: WatchFace API with custom renderer
- **Data Sync**: Wearable Data Layer API for real-time synchronization
- **Rendering**: Hardware-accelerated Canvas rendering

## Project Structure

```
custom-vibe-watch-faces/
├── mobile/                          # Mobile companion app
│   └── src/main/
│       ├── java/com/vibewatch/companion/
│       │   ├── ui/                  # Compose UI screens
│       │   ├── viewmodel/           # ViewModels
│       │   ├── data/                # API services & repositories
│       │   └── MainActivity.kt
│       └── res/                     # Resources
├── wear/                            # WearOS watch face app
│   └── src/main/
│       ├── java/com/vibewatch/watchface/
│       │   ├── service/             # Watch face service
│       │   └── renderer/            # Custom renderer
│       └── res/                     # Resources
└── build.gradle.kts
```

## Customization Options

### Font Styles (Digital)
- **BOLD**: Strong, prominent digits
- **LIGHT**: Thin, elegant appearance
- **MONO**: Monospaced, technical look
- **SERIF**: Classic, traditional style
- **NORMAL**: Standard sans-serif font

### Dial Styles (Analog)
- **CLASSIC**: Traditional triangular hands
- **MODERN**: Rounded, minimalist hands
- **SWORD**: Sharp, blade-like hands
- **MINIMAL**: Thin, simple lines
- **ORNATE**: Decorative, detailed hands

### Background Styles
- **GRADIENT**: Linear color transition
- **RADIAL**: Circular gradient from center
- **NATURE**: Nature-inspired elements
- **METALLIC**: Shiny, reflective appearance
- **ABSTRACT**: Geometric artistic patterns

## Technical Details

### AI Integration
The app uses Google's Gemini Pro model to:
1. Parse natural language prompts
2. Extract design parameters (colors, styles, watch type)
3. Generate appropriate color schemes
4. Determine layout and styling preferences

### Watch Face Generation
- Resolution: 512x512 pixels (scaled to watch size)
- Format: PNG with transparency support
- Rendering: Hardware-accelerated Canvas API
- Update frequency: Real-time (60 FPS in active mode)

## Troubleshooting

### "Please set your Gemini API key"
- Make sure you've entered a valid API key in Settings
- Check that the key is correctly copied without extra spaces

### "Failed to generate image"
- Verify your internet connection
- Check if the Gemini API is accessible in your region
- Ensure your API key quota hasn't been exceeded

### "No connected watch found"
- Make sure your WearOS watch is paired and connected
- Check Bluetooth connection
- Ensure the wear app is installed on your watch

### Watch face not updating
- Try syncing again from the mobile app
- Restart the watch face on your watch
- Check if the Wearable Data Layer is enabled

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Google Gemini AI for intelligent prompt parsing
- WearOS team for the comprehensive watch face APIs
- Jetpack Compose for modern UI development

## Contact

For questions or support, please open an issue on GitHub.

---

**Note**: This app uses AI to generate visual parameters and creates watch faces locally on your device. No user data or watch face designs are stored or transmitted to any server except for the API call to Google Gemini for prompt analysis.
