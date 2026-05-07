# PesaLens - Advanced M-Pesa Transaction Analytics

A powerful Android app for tracking, analyzing, and understanding your M-Pesa and mobile money transactions. **All data stays on your device** - no cloud uploads, no tracking.

## 🎯 Features

### Core Functionality
- 🔐 **Biometric Security** - Fingerprint/Face ID protection
- 📊 **Transaction History** - Complete SMS-based transaction tracking
- 💰 **Smart Analytics** - Expense and income breakdowns by provider
- 🔄 **Multi-Provider Support** - Safaricom, Airtel, Telkom, Faiba tracking
- 🎨 **Theme Support** - Light/Dark/System themes
- 🌍 **Multi-Currency** - Display in KES, USD, EUR, GBP, UGX, TZS, ZAR, NGN, GHS
- 🔍 **Privacy Mode** - Blur sensitive amounts in screenshots

### Advanced Trackers

#### 💳 Fuliza Health Tracker
- Track current Fuliza usage and repayment status
- Monitor days spent in overdraft
- Daily/weekly usage warnings
- Repayment recommendations

#### 🏪 Paybill/Till Auto-Categories
- Auto-detect and categorize transactions:
  - KPLC, Naivas, Quickmart, Carrefour
  - Uber, Bolt, Safaricom, Airtel
  - School fees, Banks, Rent, Betting sites
  - And more...
- Remember your corrections
- Custom merchant naming

#### ⚡ KPLC Token Tracker
- Detect prepaid token purchases
- Track units bought and cost per unit
- Monthly electricity spending analysis
- Expected next purchase date

#### 📱 Airtime/Data Bundle Tracker
- Separate tracking per provider:
  - Safaricom, Airtel, Telkom, Faiba
- Identify which line drains money
- Monthly airtime vs data split
- Top spender alerts

#### 📅 Rent & Bills Calendar
- Auto-detect recurring payments:
  - Rent, WiFi, Water, School Fees
  - Chama, SACCO, Loans
- Payment reminders before due dates
- Overdraft notifications
- Monthly obligations summary

#### 👥 Chama/SACCO Tracker
- Record monthly contributions
- Track welfare payments
- Monitor loans taken and disbursed
- Track guarantor obligations
- Missed contribution alerts

#### 👨‍👩‍👧 Family Support Tracker
- Group recurring family support
- Track by recipient relationship
- Monthly trend analysis
- "Support Pressure" warnings
- Family budget breakdowns

#### 🚕 Matatu/Transport Budget
- Tag daily transport spending
- Weekly commute cost calculation
- Transport mode breakdown (Matatu, Uber, Bolt)
- Commuting pattern insights
- Budget optimization tips

#### 🎰 Betting & Risky Spend Detection
- Detect betting platform transactions
- Track total staked vs wins
- Net loss calculation
- Monthly trend warnings
- Platform breakdown

#### 💾 M-Pesa Statement Import
- Import deleted SMS messages
- CSV/PDF M-Pesa statement support
- Merge with existing SMS data
- Historical data recovery

#### 🏷️ Merchant Cleanup Rules
- Rename confusing merchant names
- Save custom names per paybill
- One-time corrections, remember forever
- "Rent", "KPLC", "Mama Mboga" clarity

#### 🎯 Monthly Survival Number
- Calculate required daily spending
- "You need Ksh X to finish this month"
- Based on your actual spending patterns
- Daily budget recommendations

#### 🎫 Black Tax / Support Pressure View
- Track all support obligations
- See "Black Tax" percentage of income
- Rising pressure detection
- Non-judgmental, practical insights

#### 💼 Offline AI Advisor (No API Required)
- Local transaction analysis
- Budget warnings
- Top spenders summary
- Fuliza advice
- Spending insights (no cloud needed)
- Optional Claude API for advanced analysis

#### 👛 Multiple Lines/Wallets View
- Track Safaricom + Airtel/Telkom/Faiba
- Combined spending totals
- Per-provider breakdowns
- Easy switching between accounts

---

## 📥 Download

### ⭐ Get the Latest APK from GitHub Releases

The easiest way to install PesaLens is to download the pre-built APK from the GitHub releases page.

**[👉 DOWNLOAD APK HERE 👈](https://github.com/MechTinXel/Pesa-Lens/releases)**

### Installation Steps
1. **Download** - Open the latest release and download `app-debug.apk` or `app-release.apk`
2. **Enable Unknown Sources** - Go to Settings > Apps > Special app access > Install unknown apps > Enable for your browser/file manager
3. **Install** - Tap the downloaded APK file and select "Install"
4. **Grant Permissions** - Allow SMS Read and other permissions when prompted
5. **Set Security** - Register your biometric (fingerprint/face ID) on first launch
6. **Start Tracking** - Your transactions will load automatically from SMS history! 🎉

### Requirements
- ✅ Android 8.0 (API 26) or higher
- ✅ SMS Read Permission (for transaction parsing)
- ✅ Biometric capability (fingerprint/face recognition)

### Troubleshooting
- **"Unknown app" warning?** - This is normal. Tap "Install anyway" or "I understand the risk"
- **APK won't install?** - Check your Android version is 8.0+
- **No transactions showing?** - Grant SMS Read permission in Settings > Apps > PesaLens > Permissions

---

## 🚀 Getting Started

### Installation
📖 **[Read the detailed Installation Guide →](./INSTALLATION.md)**

Quick start:
1. Download APK from [GitHub Releases](https://github.com/MechTinXel/Pesa-Lens/releases)
2. Enable "Unknown Sources" in Android Settings
3. Install the APK
4. Grant SMS and biometric permissions
5. Done! ✓

### Navigation
- **History** - All transactions chronologically
- **Expenses** - Breakdown of money sent
- **Income** - Breakdown of money received
- **Analytics** - Charts and trends
- **AI Advisor** (offline mode by default)
- **Settings** - Toggle Income/Expenses, theme, currency, API key

### Settings
- **Theme Mode** - Light, Dark, System
- **Display Currency** - All transactions converted for display
- **Show Income** - Toggle income transactions visibility
- **Show Expenses** - Toggle expense transactions visibility
- **AI Advisor API Key** - Optional Claude API for cloud analysis
- **Privacy & Data** - Biometric lock, on-device storage info

---

## 🔒 Privacy & Security

✅ **Zero Cloud Upload** - All data stays on your phone
✅ **Encrypted Storage** - Sensitive data encrypted locally
✅ **No Tracking** - No analytics, no telemetry
✅ **Biometric Lock** - Fingerprint/Face ID required to open
✅ **Read-Only SMS** - Only reads SMS, never modifies
✅ **Optional AI** - Cloud features only if you add an API key

---

## 🛠️ For Developers

### Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM with coroutines
- **Storage:** DataStore for preferences, encrypted storage for sensitive data
- **API:** On-device processing + optional Claude AI

### Building & Releasing
📖 **[Read the Build Guide →](./BUILD.md)**

**Quick Build:**
```bash
git clone https://github.com/MechTinXel/Pesa-Lens.git
cd PesaLens

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Automated Releases
- ✅ Push to `main` → Auto-builds and creates "latest" release
- ✅ Create git tag (`git tag v1.0.0`) → Auto-builds and creates version release
- ✅ APKs automatically uploaded to GitHub Releases

### Key Files
- `MainActivity.kt` - Main UI and screen composition
- `logic/BudgetEngine.kt` - Core analytics
- `logic/*Tracker.kt` - Feature implementations
- `ui/screens/` - Individual screen implementations
- `data/SettingsRepository.kt` - User preferences

---

## 📊 Roadmap

### Upcoming Features
- [ ] PesaLink integration for real-time balance
- [ ] Recurring expense auto-detection
- [ ] Budget alerts and push notifications
- [ ] Custom reports and exports
- [ ] Wealth tracking (goal setting)
- [ ] Integration with mobile banking apps
- [ ] Google Drive backup (encrypted)
- [ ] Widget for quick stats

---

## 🤝 Contributing

Contributions welcome! For major changes:
1. Fork the repo
2. Create a feature branch
3. Test thoroughly
4. Submit a PR with description

---

## 📝 License

This project is open source - use, modify, and share freely.

---

## 🙏 Acknowledgments

Built for Kenyans understanding their money, simplified.

**Special thanks to:**
- SMS-based parsing inspiration from community tools
- Compose Material3 design system
- Everyone tracking their Fuliza and transport budgets

---

## 📧 Support

Found a bug? Have suggestions? 
- Open an issue on GitHub
- Check existing issues first

---

## ⚡ Quick Tips

1. **First Run Slow?** - Initial SMS sync takes ~2 min depending on message count
2. **Permission Denied?** - Grant SMS Read in Android Settings > Apps > PesaLens
3. **Can't See Data?** - Ensure notifications contain transaction details (not just "M-Pesa sent")
4. **Privacy Blur?** - Toggle privacy mode with eye icon on dashboard
5. **New Provider?** - Switch providers in filter dropdown, Fuliza auto-hides for non-Safaricom

---

**Happy Money Tracking! 🎉**

*TinXel work as play*


