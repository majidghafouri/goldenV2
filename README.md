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
5. App starts `VpnServiceImpl` foreground service
6. Service establishes TUN interface and starts Xray

### Manifest Requirements

The following permissions and components are declared in `core/vpn/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<service
    android:name=".service.VpnServiceImpl"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="true"
    android:foregroundServiceType="specialUse">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>

<receiver
    android:name=".receiver.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### Key Integration Points (TODO Markers)

Search for `// TODO:` comments in these files:

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

4. **`feature/home/HomeViewModel.kt`**:
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