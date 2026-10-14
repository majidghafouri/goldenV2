# GoldenV2 - V2Ray/Xray Proxy Client for Android

A native Android V2Ray/Xray proxy client built with modern Android development practices.

## Features

- **Subscription Management**: Add V2Ray subscriptions via URL, auto-refresh with WorkManager
- **Server Management**: Parse vmess://, vless://, trojan://, ss://, hysteria2:// links
- **Connection Core**: VpnService-based TUN interface with Xray-core integration
- **Routing Rules**: Rule-based routing with bypass LAN, GeoIP, custom rules
- **Settings**: DNS, local proxy, theme, kill switch, auto-connect, import/export
- **Logs & Diagnostics**: Real-time log viewer, speed test, ping test

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with clean layering (UI/Domain/Data)
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Networking**: Retrofit + OkHttp
- **Serialization**: kotlinx.serialization
- **Async**: Coroutines + Flow/StateFlow
- **Background**: WorkManager

## Project Structure

```
GoldenV2/
├── app/                    # Main application module
├── core/
│   ├── data/              # Data layer (Room, DataStore, Repositories)
│   ├── domain/            # Domain layer (Models, UseCases, Repository interfaces)
│   ├── network/           # Network layer (Retrofit, Config parsers)
│   ├── ui/                # Shared UI components & theming
│   └── vpn/               # VPN service & Xray integration
├── feature/
│   ├── home/              # Home screen (connect toggle, stats)
│   ├── servers/           # Server list & management
│   ├── routing/           # Routing rules editor
│   ├── settings/          # Settings screen
│   └── logs/              # Log viewer & diagnostics
└── gradle/
    └── libs.versions.toml # Version catalog
```

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17+
- Android SDK 35 (compileSdk), minSdk 24

### Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build the project (`./gradlew assembleDebug`)

### Adding Xray-core (Required for VPN Functionality)

**⚠️ IMPORTANT**: The VPN functionality requires the Xray-core binary. This is NOT included in the repository.

#### Option 1: Build Xray-core from Source

```bash
# Clone Xray-core
git clone https://github.com/XTLS/Xray-core.git
cd Xray-core

# Build for Android (requires Go toolchain and Android NDK)
go build -buildmode=c-shared -o libxray.so ./main
```

#### Option 2: Use v2rayNG's Core Wrapper

v2rayNG provides a pre-built wrapper. See their [releases](https://github.com/2dust/v2rayNG/releases).

### VPN Permission Flow

1. User taps "Connect" on Home screen
2. App calls `VpnService.prepare(context)` 
3. System shows VPN permission dialog
4. User grants permission
5. App starts `VpnServiceImpl` (a `VpnService`, not a foreground service)
6. Service establishes TUN interface and starts Xray

### Manifest Requirements

The following permissions and components are declared in `core/vpn/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<service
    android:name=".service.VpnServiceImpl"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

No `FOREGROUND_SERVICE` permission and no `foregroundServiceType` are declared. The service
does not call `startForeground()`: it establishes a TUN device and posts a standard
`NotificationManager` notification, which Android shows for the lifetime of a `VpnService`.
Declaring a foreground-service type here is unnecessary and would require the matching
permission plus a Play `specialUse` justification.
<receiver
    android:name=".receiver.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### Key Integration Points

1. **`core/vpn/service/VpnServiceImpl.kt`**:
   - `findXrayBinary()` - Locate Xray binary
   - `startXrayProcess()` - Start Xray with config
   - `parseStatsLine()` - Parse traffic stats from Xray
   - `addExcludedRoutes()` - Handle routing exclusions

2. **`core/vpn/xray/XrayConfigBuilder.kt`**:
   - Complete Xray JSON config generation
   - All protocol outbound configurations
   - Routing rules translation

3. **`core/network/parser/ConfigParser.kt`**:
   - Additional protocol parsers if needed
   - Subscription content parsing

4. **`core/network/parser/ServerUriSerializer.kt`**:
   - Share/export URIs per protocol (inverse of the parsers)
   - Hysteria (v1), Tuic, and WireGuard URIs are generated but not re-parseable yet (`ConfigParserRegistry` has no parsers for them)

5. **`feature/home/HomeViewModel.kt`**:
   - VPN permission request handling

## Architecture Overview

### Data Flow

```
UI (Compose) → ViewModel → UseCase → Repository → DataSource (Room/DataStore/Network)
                    ↑                                          ↓
              StateFlow ←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←←
```

### VPN Service Communication

```
HomeViewModel → VpnController (interface) → VpnServiceImpl (VpnService)
                                              ↓
                                    Foreground Service + Notification
                                              ↓
                                    Xray Process / Native Library
```

### Subscription Refresh

```
WorkManager (periodic) → SubscriptionFetcher → ConfigParser → ServerRepository
                                                      ↓
                                            Update UI via Flow
```

## Configuration

### AppSettings (DataStore)

Stored in `SettingsDataStore` with keys:
- `settings_json` - Full AppSettings serialized as JSON
- `first_run` - Boolean for onboarding
- `last_selected_server_id` - Last connected server

### Database Schema (Room)

**servers table**:
- id (PK), subscriptionId, name, protocol, address, port, uuid, password, etc.
- latency, isSelected, createdAt, updatedAt

**subscriptions table**:
- id (PK), url, name, remark, isEnabled, autoRefresh, refreshIntervalHours
- lastRefreshAt, lastRefreshSuccess, lastRefreshError, serverCount

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Specific module tests
./gradlew :core:network:test
./gradlew :core:domain:test
```

## Ads & Analytics

The app shows a banner on Home and an interstitial before each connect. Ad
networks are abstracted behind an `AdProvider` interface in
`feature/home/.../ads/`, and `AdsManager` picks the first one that can actually
serve:

1. **Google AdMob** (primary) — skipped when it is not configured, when the
   device has no Google Play Services, or when it stops filling
2. **Yandex Mobile Ads** (fallback) — works on every device
3. **`NoAdsProvider`** — no ads; the app stays fully usable

`AdsManager` never blocks the VPN connect flow: if no ad is ready, or loading or
showing fails, the connect proceeds immediately.

### Configuration

Keys are read from `local.properties` (gitignored) at build time:

```properties
# local.properties
ADMOB_ENABLED=true
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_BANNER_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
ADMOB_INTERSTITIAL_AD_UNIT_ID=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
ADMOB_TEST_DEVICE_IDS=                     # optional, debug builds only

YANDEX_BANNER_AD_UNIT_ID=R-M-XXXXXXX_Y
YANDEX_INTERSTITIAL_AD_UNIT_ID=R-M-XXXXXXX_I
APPMETRICA_API_KEY=<your AppMetrica API key>
```

The AdMob App ID is injected into the manifest as `${admobAppId}`, defaulting to
Google's public sample ID when `ADMOB_APP_ID` is empty. Since AdMob is reported
as unavailable in that case, the sample ID is never used to serve ads.

Without any Yandex keys, debug builds fall back to the public Yandex demo ad
unit IDs, so the repo stays buildable out of the box.

### How the fallback works

- **Not configured** — `ADMOB_APP_ID` or `ADMOB_INTERSTITIAL_AD_UNIT_ID` is blank, or
  `ADMOB_ENABLED=false`. The interstitial is the primary placement and decides whether AdMob
  is usable; `ADMOB_BANNER_AD_UNIT_ID` is optional, and until it exists the Home banner is
  simply not rendered while interstitials keep serving.
- **No Play Services** — `AdmobAdProvider.isAvailable()` checks
  `GoogleApiAvailability.isGooglePlayServicesAvailable()`. AdMob cannot fill
  without Play Services, so this is what covers devices in markets where GMS is
  absent.
- **No fill** — `AdFrequencyManager` aside, the active network reports failed
  interstitial loads; after `MAX_CONSECUTIVE_LOAD_FAILURES` (3) `AdsManager`
  demotes it to the next network. Banner no-fill is ignored, since it is common
  and does not affect the interstitial.

### User Consent & Privacy

On first launch the app shows a data-processing consent dialog (required for
EEA users). The answer is persisted in SharedPreferences and wired to every SDK:

- **Google AdMob**: `RequestConfiguration.setPublisherPrivacyPersonalizationState()`
  — declined = `DISABLED`, which opts every request out of personalized ads
  (ads still serve either way). This is AdMob's non-personalized-ads opt-out.
- **Yandex Mobile Ads**: `YandexAds.setUserConsent()` — granted = personalized
  ads, declined = non-personalized/contextual ads
- **AppMetrica**: `setDataSendingEnabled()` — data is **not sent at all** until
  the user accepts. A fresh install starts in a fully opt-out state.

`ConsentManager` holds no ad SDK types, so it can be injected from `:app` without
pulling an ad SDK into the application module.

The `com.google.android.gms.permission.AD_ID` permission (declared in the app
manifest) lets the ad SDK access the advertising ID on Android 13+.

### Push Notifications (optional)

AppMetrica Push requires a Firebase project:

1. Create a Firebase project and add `google-services.json` to the `app/` module
2. Apply the `com.google.gms.google-services` Gradle plugin
3. AppMetrica Push activation is already wired in `GoldenV2Application`

Without Firebase, `AppMetricaPush.activate()` is a safe no-op.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and lint
5. Submit a PR

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Acknowledgments

- [Xray-core](https://github.com/XTLS/Xray-core) - The underlying proxy engine
- [v2rayNG](https://github.com/2dust/v2rayNG) - Reference Android implementation

## Disclaimer

This app is for educational purposes. Users are responsible for complying with local laws and regulations regarding proxy/VPN usage.