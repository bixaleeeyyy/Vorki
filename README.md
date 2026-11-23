# Vorki

Customisable home-screen widgets for Android. Free software, no ads, no tracking, no internet permission.

## Widgets
- **Clock** – 12 styles (digital and analog), 8 fonts, a photo clock with the time over your own picture.
- **Calendar** – date, date + day, or the whole month.
- **Counters** – countdown, days since, and year / month / week progress.
- **Event list** – your next dates in one list.
- **Notes** – sticky note or to-do list, with paper styles.
- **Quote of the day** – built-in quotes and your own.
- **Photo frame** – one photo or a slideshow of up to 12.
- **Quick Tap** – double-tap to lock the screen, open an app, toggle the flashlight or open search.
- **App row** – up to six app shortcuts.
- **Fun** – dice, coin, magic 8-ball, rock-paper-scissors, random number, pick from your list.

Every widget can use a picture from your gallery as its background. There are five app colour palettes
(Aurora is the default), about twenty colour swatches plus a custom colour picker, and eight built-in fonts.

## Build
Open the inner `Vorki` folder in Android Studio (use JVM 21 for Gradle), or run `./gradlew assembleDebug`
(needs the Android SDK). The APK ends up in `app/build/outputs/apk/debug/`.

## Adding a new widget
Each widget lives in its own package under `app/src/main/java/com/vorki/app/`:
a provider (extends `VorkiProvider` – it only has to draw the widget) and a settings screen
(extends `BaseWidgetActivity` – it only has to list its options). Then:
1. add the provider and the screen to `AndroidManifest.xml`,
2. add an info file in `res/xml/`,
3. add one line to `WidgetCatalog.java`.

## AI-assisted development
Large parts of this project were written with the help of an AI assistant (Claude by Anthropic) and
reviewed and tested by the maintainer.

## Licence
GNU General Public License v3.0 or later – see `LICENSE`.
Vorki is based on *Transparent Widget* (c) 2016 Vishnu, EasWareApps, also GPL v3: https://gitlab.com/easwareapps/TransparentWidget
