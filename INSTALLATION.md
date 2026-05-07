# PesaLens Installation Guide 📱

## Quick Download

The fastest way to get PesaLens on your phone:

### Step 1: Download the APK
1. Go to [GitHub Releases](https://github.com/MechTinXel/Pesa-Lens/releases)
2. Look for the **latest release**
3. Download the file named `app-debug.apk` or `app-release.apk`

### Step 2: Enable Installation from Unknown Sources
1. Open your **Android Settings**
2. Go to **Apps & Notifications** (or "Apps" on some devices)
3. Tap **Special app access**
4. Select **Install unknown apps**
5. Choose your **Browser** or **File Manager**
6. Toggle it **ON** ✓

### Step 3: Install the APK
1. Open your **file manager** and find the downloaded APK
2. Tap the file to open it
3. Tap **Install** (ignore any "Unknown app" warnings)
4. Wait for installation to complete (~10-30 seconds)

### Step 4: Launch PesaLens
1. Go to your **App Drawer** or **Home Screen**
2. Find and tap **PesaLens** icon
3. Grant permissions when prompted:
   - ✅ **Allow SMS permissions** (to read M-Pesa messages)
   - ✅ **Allow biometric permissions** (for security)
4. Register your **fingerprint or face ID**
5. Done! 🎉

---

## Detailed Steps by Device

### For Samsung Devices
```
Settings > Apps > App permissions > SMS
Find PesaLens and toggle SMS ON
```

### For Google Pixel/Stock Android
```
Settings > Apps & notifications > Permissions > SMS
Find PesaLens and select "Allow"
```

### For Xiaomi
```
Settings > Apps > Permissions > SMS
Find PesaLens and toggle ON
```

---

## Troubleshooting

### ❌ "Unknown app" or "Unsafe" Warning
- This is **normal** for APKs from unknown sources
- Tap **"Install anyway"** or **"I understand the risk"**
- PesaLens is safe - all code is open source on GitHub

### ❌ "Cannot install on this device"
- Check your **Android version is 8.0 or higher**
  - Go to Settings > About Phone > Android Version
- Try downloading from a different browser if blocked

### ❌ No transactions showing after install
1. Grant **SMS permissions** explicitly:
   - Settings > Apps > PesaLens > Permissions > SMS > Allow
2. Check that you have **M-Pesa/Airtel/Telkom messages** in your inbox
3. Try **pull-to-refresh** on the History tab
4. Restart the app

### ❌ "Installation blocked by Play Protect"
- Go to **Google Play Store > My apps > Settings**
- Look for **"Play Protect" settings**
- Toggle **"Scan apps with Play Protect" OFF** temporarily
- Install the APK
- Turn it back ON when done

---

## System Requirements

✅ **Required:**
- Android 8.0 (API 26) or higher
- ~50MB free storage space
- SMS Read permission
- Biometric sensor (fingerprint/face ID)

✅ **Optional:**
- Internet connection (only for AI features with API key)
- Google Drive (for encrypted backups)

---

## Update Guide

When new versions are released:

1. Download the latest APK from [GitHub Releases](https://github.com/MechTinXel/Pesa-Lens/releases)
2. Install it the same way as before
3. Your old version will be **replaced** (no uninstall needed)
4. All your settings and permissions carry over ✓

---

## Security & Privacy

🔒 **Your data is safe because:**
- All data stays on your phone (no cloud upload)
- Only SMS is read, nothing is modified
- Biometric lock prevents unauthorized access
- Open source code - anyone can audit it

🔗 **Optional Cloud Features:**
- AI analysis requires adding your own Claude API key
- Only enabled if YOU provide the key
- Defaults to offline-only mode

---

## Getting Help

Found a bug? Have suggestions?

👉 [Open an issue on GitHub](https://github.com/MechTinXel/Pesa-Lens/issues)

Check existing issues first - your problem might already be documented!

---

**Happy Money Tracking! 💰**

*TinXel work as play*

