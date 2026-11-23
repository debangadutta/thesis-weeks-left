# Thesis Weeks Left — Android widget

A home-screen widget counting down to thesis submission (28 April 2027): whole weeks, then days once 60 or fewer are left. No app screen, no permissions, no network.

## Install
1. Open this folder in Android Studio (File > Open). Let Gradle sync.
2. Connect your phone with USB debugging enabled.
3. Run > Edit Configurations > app > Launch: **Nothing** (the app has no screen), then Run.
4. Long-press your home screen > Widgets > "Thesis Weeks Left" > drag it on.

## Change the deadline
Edit `DEADLINE` in `app/src/main/java/de/thesis/weeksleft/WeeksLeftWidget.kt`
and the caption in `app/src/main/res/values/strings.xml`.
