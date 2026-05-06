# Changelog

All notable changes to PesaLens will be documented in this file.

## [2.0] - 2026-05-06

### 🎉 Major Features Added

#### UI/UX Improvements
- **Dropdown Filters**: Provider and year filters now use dropdowns instead of horizontal scroll (saves space)
- **Repositioned Navigation**: 
  - TinXel logo moved to center of header
  - Settings icon moved to top-right (no longer in navigation bar)
  - Privacy toggle remains on the left
- **Smart Fuliza Display**: Fuliza allowance card only shows when viewing Safaricom (hidden for other providers)
- **GitHub Actions CI/CD**: Automatic APK building and release distribution

#### Income/Expense Toggles
- Add/remove "Money In" transactions from visibility in Settings
- Add/remove "Money Out" transactions from visibility in Settings
- Useful for focusing on either income or expenses

#### Advanced Tracking Features

##### 💳 Fuliza Health Tracker
- Monitor current Fuliza usage vs limit
- Track repayment status
- Count days spent in overdraft
- Smart warnings (e.g., "You are using Fuliza 12 days this month")

##### 🏪 Paybill Auto-Categories
- Auto-detect transaction categories:
  - **Utilities**: KPLC electricity
  - **Shopping**: Naivas, Quickmart, Carrefour
  - **Transport**: Uber, Bolt
  - **Education**: School Fees
  - **Financial**: Banks
  - **Housing**: Rent agents
  - **Entertainment**: Betting sites (Betika, SportyBet)
  - **Telecom**: Airtime, internet bundles
  - **Community**: Chama, SACCO
  - **And more...**
- Remember corrections (set once, remember forever)
- Custom merchant naming

##### ⚡ KPLC Token Tracker
- Detect prepaid electricity token purchases
- Track units bought and cost per unit
- Monthly electricity spend analysis
- Estimate next purchase date

##### 📱 Airtime/Data Bundle Tracker
- Separate tracking per provider:
  - Safaricom, Airtel, Telkom, Faiba
- Identify which SIM card drains money fastest
- Monthly breakdown: airtime vs data vs internet

##### 📅 Rent & Bills Calendar
- Auto-detect recurring payments
- Categories: Rent, WiFi, Water, School Fees, Chama, SACCO, Loans
- Payment reminders before due dates
- Track overdue payments
- Monthly obligations summary

##### 👥 Chama/SACCO Tracker
- Record contributions and withdrawals
- Track welfare payments
- Monitor loans given and received
- Track guarantor obligations
- Detect missed contributions

##### 👨‍👩‍👧 Family Support Tracker
- Group recurring family transfers
- Track by relationship (Mother, Father, Siblings, etc.)
- Monthly trend analysis
- "Support Pressure" detection
- See family support as its own budget line

##### 🚕 Matatu/Transport Budget
- Tag daily transport spending
- Weekly commute cost calculation
- Transport mode breakdown (Matatu, Uber, Bolt, Taxi)
- Commuting pattern insights
- Budget optimization advice

##### 🎰 Betting & Risky Spend Detection
- Detect betting platform transactions
- Track total staked vs wins received
- Net loss calculation
- Monthly trend warnings
- Platform-specific breakdown

##### 🏷️ Merchant Cleanup Rules
- Rename confusing merchant names
- Examples: Paybill 12345 → "Rent", 54321 → "KPLC"
- One-time corrections, remember forever
- Most-used cleanups first
- Export/import rules

##### 🎯 Monthly Survival Number
- Calculate "You need Ksh X to finish this month"
- Based on your actual spending patterns
- Updated as month progresses
- Daily budget recommendations

##### 🎫 Black Tax / Support Pressure View
- Track all support obligations total
- Calculate "Black Tax" percentage of income
- Rising pressure detection
- Non-judgmental, practical insights
- Warning when support > 30% income

##### 💼 Offline AI Advisor
- Local transaction analysis (no API required)
- Budget warnings and suggestions
- Top spenders summary
- Fuliza advice
- Spending insights
- Optional Claude API integration for advanced analysis

##### 👛 Multiple Lines/Wallets View
- Track Safaricom + Airtel/Telkom/Faiba separately
- Combined spending totals across providers
- Per-provider breakdowns
- Easy switching between accounts

### 🛠️ Technical Improvements

#### Build System
- Enable ProGuard minification for release builds
- Enable resource shrinking for smaller APK
- Proper signing configuration for CI/CD
- GitHub Actions workflow for automatic releases

#### Settings Repository
- New preferences: `showIncome`, `showExpenses`
- Proper boolean preference handling
- Coroutine-friendly suspend functions

#### Code Structure
- New tracker modules in `logic/` package:
  - `FulizaHealthTracker.kt`
  - `PaybillAutoCategories.kt`
  - `KPLCTokenTracker.kt`
  - `AirtimeDataBundleTracker.kt`
  - `RentAndBillsCalendar.kt`
  - `ChamaOrSACCOTracker.kt`
  - `FamilySupportTracker.kt`
  - `TransportBudgetTracker.kt`
  - `BettingAndRiskySpendDetector.kt`
  - `MerchantCleanupAndMultiWallet.kt`
  - `BlackTaxAndSurvivalTracker.kt`

#### Documentation
- Comprehensive README.md with feature descriptions
- Getting started guide
- Privacy & security statement
- Developer build instructions

### 📥 Distribution

- GitHub Actions CI/CD pipeline
- Automatic APK building on tag push
- Release artifacts available on GitHub Releases
- Direct download for end users

---

## [1.0] - 2026-05-05

### Initial Release ✨

#### Core Features
- SMS-based M-Pesa transaction tracking
- Multi-provider support (Safaricom, Airtel, Telkom, Faiba)
- Biometric security (Fingerprint/Face ID)
- Transaction history view
- Income vs Expenses analytics
- Multi-currency support (KES, USD, EUR, GBP, UGX, TZS, ZAR, NGN, GHS)
- Privacy mode (blur sensitive amounts)
- Light/Dark/System theme support
- Theme preferences saving
- Currency preference saving

#### Screens
- History: Chronological transaction list
- Expenses: Breakdown of money sent
- Income: Breakdown of money received
- Analytics: Charts and trends
- Settings: Preferences and configurations

#### Security
- On-device data storage only
- Biometric lock on app launch
- Encrypted sensitive data storage
- SMS read-only permission

---

## Future Roadmap

### v2.1
- [ ] PesaLink integration for real-time balance
- [ ] Recurring expense auto-detection refinements
- [ ] SMS statement import (CSV/PDF support)
- [ ] Push notifications for bills and reminders

### v3.0
- [ ] Budget alerts and custom thresholds
- [ ] Wealth tracking (goal setting)
- [ ] Custom reports and exports
- [ ] Google Drive backup (encrypted)
- [ ] Widget for quick stats
- [ ] Mobile banking app integration

### Planned Features
- Deep linking for quick actions
- Offline maps for nearby spending
- Receipt scanning and OCR
- Cryptocurrency tracking
- Investment portfolio tracking
- Insurance recommendation engine

---

## Notes for Users

### Breaking Changes
None - v2.0 is fully compatible with v1.0 data.

### Migration Guide
Simply update the app - all your existing transactions will be imported and enhanced with new tracker categories automatically.

### Known Limitations
- Initial SMS import may take 2-3 minutes on devices with large SMS history (1000+ messages)
- KPLC token cost-per-unit is estimated; actual parsing requires full SMS content
- Recurring payment detection uses pattern matching; manual confirmation recommended

---

## Contributors

- **Felix Muting'e** (@MechTinXel) - Lead Developer

---

**Last Updated**: May 6, 2026

For issues and feature requests, visit [GitHub Issues](https://github.com/MechTinXel/Pesa-Lens/issues)

