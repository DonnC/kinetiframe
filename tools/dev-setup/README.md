# Developer setup on macOS

How to build KinetiFrame on a Mac and check it on emulators of several sizes and on a real tablet.
It assumes Android Studio, a JDK, Git and an editor are already installed.

## 1. Get the code

```sh
git clone https://github.com/DonnC/kinetiframe.git animation
cd animation
git switch develop
```

Each milestone gets its own worktree next to the main checkout, as on any machine:

```sh
git worktree add ../kf-<milestone> -b milestone/<milestone> develop   # a new milestone
git worktree add ../kf-essentials milestone/ux-round-2                  # an existing branch
```

The build needs JDK 17 (`KinetiFrameBuild.JAVA`). In Android Studio, Settings › Build, Execution,
Deployment › Build Tools › Gradle › Gradle JDK, pick a 17 (the bundled JBR works). On the command
line, point `JAVA_HOME` at a 17 or let Gradle's toolchain find one. Android Studio writes
`local.properties` with `sdk.dir`; for command-line builds, set `ANDROID_HOME` in your shell:

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

Then check the build:

```sh
./gradlew spotlessApply verify
```

## 2. Emulators

In Android Studio › Settings › Languages & Frameworks › Android SDK › SDK Tools, tick **Android SDK
Command-line Tools (latest)** and **Android Emulator**. Then:

```sh
tools/dev-setup/emulators.sh install   # system images for API 37.0 and API 28 (once)
tools/dev-setup/emulators.sh create    # kf-phone-small, kf-phone, kf-foldable,
                                       # kf-tablet-small, kf-tablet, kf-phone-oldest
tools/dev-setup/emulators.sh start kf-tablet kf-phone
tools/dev-setup/emulators.sh install-app   # build once, install on every running device
```

| AVD | Profile | Why |
|---|---|---|
| kf-phone-small | small_phone | Cramped layouts and cut-off labels |
| kf-phone | pixel_8 | The common case |
| kf-foldable | pixel_9_pro_fold | Folding and unfolding mid-session, two-pane widths |
| kf-tablet-small | small_tablet | 7-inch tablet in both orientations |
| kf-tablet | pixel_tablet | 11-inch tablet, landscape drawing |
| kf-phone-oldest | pixel_4 at API 28 | The oldest Android KinetiFrame supports |

On Apple silicon the script uses arm64 images, which run at near-native speed. A profile your
emulator version doesn't know is skipped with a message; pick another from
`avdmanager list device -c` and edit `PROFILES` in the script.

Emulators can't test pen pressure, palm rejection or real drawing feel. Check those on the
physical tablet and phones.

## 3. A real tablet or phone

USB: turn on Developer options › USB debugging, plug it in, accept the prompt, then
`adb devices -l`. A USB-C cable that only charges won't show the device.

Wi-Fi (Android 11+): Developer options › Wireless debugging › Pair device with pairing code, then

```sh
adb pair <ip>:<pairing-port>     # enter the code shown on the device
adb connect <ip>:<port>          # the port on the Wireless debugging screen
```

`./gradlew connectedAndroidTest` uninstalls the dev app afterwards and wipes its projects. Pass
`-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` to keep them.

## 4. UX review personas

The personas drive any device by its adb serial, so an emulator works as well as a phone:

```sh
export KF_DEVICE=emulator-5554        # or the tablet's serial from `adb devices`
export KF_UX_OUT=notes/ux-review/$(date +%F)/<persona>
source tools/ux-review/phone.sh
```

Running the tablet persona pass on `kf-tablet` and `kf-foldable` is the quickest way to find
layouts that only break on large screens.
