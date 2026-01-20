adb shell dpm remove-active-admin com.osamaalek.kiosklauncher/.MyDeviceAdminReceiver
adb uninstall com.osamaalek.kiosklauncher
./gradlew clean build assembleDebug assembleRelease
adb install -r -t ./app/build/outputs/apk/release/app-release.apk
adb shell dpm set-device-owner com.osamaalek.kiosklauncher/.MyDeviceAdminReceiver
adb reboot