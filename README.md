# TimeTrek

TimeTrek is an Android app for tracking work time and seeing how much of a weekly goal remains. It works without an account or a background timer service.

![img_1.png](img_1.png) ![img.png](img.png)

## Features

- **Track:** Start or finish a session with one large button. An active session keeps accruing time while the app is closed; its start timestamp is saved and elapsed time is calculated when you return.
- **Weekly goal:** Set the hours and minutes you need to work. The remaining-time display updates while tracking and shows **Done!** once the goal is met. The goal stays set; the tracked weekly total starts fresh each week.
- **Progress messages:** The goal card picks a message based on how much time remains, with playful reminders to take a break when you go into overtime.
- **Calendar:** Dots mark days with tracked time. Select a date to see its total and sessions; add worktime manually or edit and delete completed sessions.

Sessions that cross midnight or a week boundary contribute only their overlapping time to each day or week. Weeks follow the device's local calendar settings. Sessions and the weekly goal are saved on the device using Android `SharedPreferences` (and may be included in Android backup, depending on device settings).

## Build and test

Open the project in Android Studio with the Android SDK installed. The app supports Android 7.0 (API 24) and newer. You can also use the included Gradle wrapper:

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. A debug APK is intended for development, **not** for publishing as a release.

## Version updates and GitHub releases

Releases are currently made manually; this repository has no automated signing or GitHub Actions release workflow.

1. **Bump the version.** In `app/build.gradle.kts`, increase `versionCode` for every new APK and set a matching, human-readable `versionName`. For example, after releasing `1.0` with code `1`:

   ```kotlin
   versionCode = 2
   versionName = "1.1"
   ```

   Keep `applicationId` unchanged so the new APK can update existing installs. `versionCode` must be higher than the installed version's code.

2. **Build and test.** Run `./gradlew testDebugUnitTest assembleDebug`, then in Android Studio choose **Build → Generate Signed App Bundle / APK → APK** and select the **release** variant. For the first release, choose **Create new** to make a `.jks` keystore. For every later release, choose **the same existing keystore and key alias**. Back up the keystore and passwords securely outside the repository; losing the key prevents users from installing future updates over their existing app.

3. **Verify the signed APK.** Use the output path shown by Android Studio (often `app/release/app-release.apk`). Confirm the APK was rebuilt after the version change. Install it over a previous release on a device to check that it upgrades successfully and retains tracked sessions and the weekly goal. Do not use a debug APK or an unsigned release APK.

4. **Publish on GitHub.** Commit and push the source changes. On the repository's **Releases** page, select **Draft a new release**, create a new tag matching `versionName` (for example, `v1.1`) on that commit, upload the signed APK, add release notes, and publish. Attach the APK as a **release asset** rather than committing it to Git.

   If you use GitHub CLI instead, run this after pushing the version change, replacing the path with your signed APK's actual location:

   ```sh
   gh release create v1.1 "app/release/app-release.apk#TimeTrek-v1.1.apk" --title "TimeTrek 1.1" --generate-notes
   ```

Never upload the keystore or passwords to GitHub. Running `./gradlew assembleRelease` without a release signing configuration is **not** a substitute for generating a signed APK.

## Code layout

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/at/oderwieoderw/timetrek/ui/` | Activity navigation and display formatting |
| `app/src/main/java/at/oderwieoderw/timetrek/ui/tracking/` | Timer, weekly goal, and start/finish controls |
| `app/src/main/java/at/oderwieoderw/timetrek/ui/calendar/` | Calendar, day history, and session editing |
| `app/src/main/java/at/oderwieoderw/timetrek/domain/` | Time calculations and goal-progress stages |
| `app/src/main/java/at/oderwieoderw/timetrek/data/local/` | Local session and goal storage |
