# Glyph Catch

A fan-made Pokémon catching game + digital clock for the Nothing Phone 3 Glyph Matrix. Leave your phone face-down for a while and Pokémon will appear. Long-press to catch them.

## Nothing Phone 3 Glyph Toys

Glyph Toys are interactive applications that run on the Glyph Matrix - a circular 25x25 LED matrix on the back of the Nothing Phone 3 (July 2025). The round display consists of 489 individually controllable LEDs (not all 625 pixels of the 25x25 grid are visible).

There is a single touch button of the back of the phone. Glyph Toys can respond to long-presses of this touch button.

## Code style

- Standard Kotlin conventions
- Minimalism
- Vertical slicing

## Commands

### Build and development

```bash
./gradlew build                 # Build app
./gradlew clean build           # Clean and rebuild app
./gradlew installDebug          # Install app on connected device
./gradlew test                  # Run unit tests
./gradlew assembleRelease       # Generate signed APK
```

### Code quality

```bash
./gradlew lint          # Run Android lint checks
./gradlew ktlintCheck   # Check Kotlin code style
./gradlew ktlintFormat  # Auto-format Kotlin code
```

### Technical details

- Target SDK: Android 15 (API 35+)
- Namespace: `dev.equalparts.glyph_catch`
- Build system: Gradle with Kotlin DSL and version catalogs

This app targets the Nothing Phone 3, so older Android version compatibility is never a factor.
