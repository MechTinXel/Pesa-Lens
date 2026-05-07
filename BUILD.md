# Build & Release Guide 🔨

## Automated Builds (GitHub Actions)

Every push to `main` and every tag push automatically triggers a build and release on GitHub!

### How It Works

1. **Push to main** → Automatic build → Creates "Latest" release with debug APK
2. **Create a version tag** → Automatic build → Creates versioned release with APKs
3. **GitHub Releases** → APK files are uploaded automatically

### Creating a Release

To create a new release, simply tag your commit:

```bash
git tag v1.0.1
git push origin v1.0.1
```

This will:
- ✅ Build the APK automatically
- ✅ Create a GitHub release
- ✅ Upload APKs to the release
- ✅ Notify watchers of the new version

---

## Building Locally

### Prerequisites

- Java 17 or higher
- Android SDK (API 26+)
- Gradle 8.0+

### Debug Build (No Signing Required)

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Requires Signing Key)

For a properly signed release APK:

#### Step 1: Generate a Keystore

```bash
keytool -genkey -v -keystore pesalens-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias pesalens-key \
  -storepass your_keystore_password \
  -keypass your_key_password \
  -dname "CN=Your Name, O=Your Organization, L=Your City, C=Your Country"
```

Keep the keystore file safe! You'll need it for all future releases.

#### Step 2: Build the Release APK

```bash
KEYSTORE_FILE=./pesalens-release.jks \
KEYSTORE_PASSWORD=your_keystore_password \
KEY_ALIAS=pesalens-key \
KEY_PASSWORD=your_key_password \
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release.apk`

---

## Setting Up Automated Signing (CI/CD)

For GitHub Actions to sign releases automatically, you need to add secrets:

### Step 1: Encode Your Keystore (Base64)

```bash
# Windows PowerShell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("./pesalens-release.jks")) | Set-Clipboard

# Or Linux/Mac
cat pesalens-release.jks | base64
```

### Step 2: Add GitHub Secrets

1. Go to GitHub → Settings → Secrets and variables → Actions
2. Add these secrets:
   - `SIGNING_KEY` - Base64 encoded keystore file
   - `SIGNING_KEY_ALIAS` - Key alias (e.g., "pesalens-key")
   - `SIGNING_KEY_PASSWORD` - Key password
   - `KEY_STORE_PASSWORD` - Keystore password

### Step 3: Workflow Automatically Signs APKs

Once secrets are set up, the GitHub Actions workflow will:
- ✅ Build the APK
- ✅ Sign it automatically
- ✅ Upload to releases

---

## Build Troubleshooting

### "Gradle build failed"
```bash
./gradlew clean
./gradlew build --stacktrace
```

### "Keystore not found"
- Ensure the keystore file path is correct
- Check environment variable names match build.gradle.kts

### "APK not created"
```bash
# Check if build was successful
ls app/build/outputs/apk/*/
```

---

## Building for Different API Levels

The app supports:
- **Minimum:** Android 8.0 (API 26)
- **Target:** Android 15 (API 35)

Adjust in `app/build.gradle.kts`:
```kotlin
minSdk = 26
targetSdk = 35  // Change this to target newer Android
```

---

## ProGuard/R8 Obfuscation

Release builds use R8 obfuscation: `app/proguard-rules.pro`

To disable obfuscation:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false  // Disabled
    }
}
```

---

## Signing Key Best Practices

⚠️ **IMPORTANT:**
- 🔐 **Keep keystore file safe!** Back it up
- 🔑 **Never commit keystore to Git**
- 📝 **Store passwords securely** (not in code)
- 🛡️ **Use GitHub Secrets** for CI/CD
- 💾 **Don't lose it!** You'll need it for all future updates

---

## Release Checklist

Before releasing a new version:

- [ ] Update version code in `app/build.gradle.kts`
- [ ] Update CHANGELOG.md
- [ ] Test on multiple devices
- [ ] Run `./gradlew lint` for warnings
- [ ] Commit changes
- [ ] Create a git tag: `git tag v1.0.X`
- [ ] Push tag: `git push origin v1.0.X`
- [ ] Verify GitHub Actions completed
- [ ] Test downloaded APK on real device

---

## Distribution Links

Once built, share APKs at:
- **GitHub Releases:** https://github.com/MechTinXel/Pesa-Lens/releases
- **Direct APK URL:** (updated with each release)

---

## CI/CD Pipeline Status

Check build status at:
https://github.com/MechTinXel/Pesa-Lens/actions

---

**Happy building! 🚀**

*For help, open an issue on GitHub*

