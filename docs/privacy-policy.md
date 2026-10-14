# Privacy Policy for GoldenV2

_Last updated: October 3, 2026_

This Privacy Policy describes how GoldenV2 ("the App", "we", "us") handles information when you use our Android application. The App is a VPN client: it connects to proxy servers that **you** configure or subscribe to. We do not operate those servers and this policy does not cover them.

## Summary

- We do **not** collect or transmit your browsing traffic, VPN destination addresses, or connection content.
- Analytics and advertising data is collected **only with your consent**, via a first-launch dialog.
- If you decline consent, no analytics data is sent and ads are requested without personalization.
- Advertising is served by Google AdMob, or by Yandex Mobile Ads on devices where AdMob is unavailable.
- VPN connection data never leaves your device, except to your chosen proxy server, as required to establish the tunnel.

## Data We Do NOT Collect

- **Browsing history and traffic content**: all traffic is routed through your configured proxy server. We have no visibility into, and do not log, what you do through the tunnel.
- **VPN server credentials**: your subscription URLs, server addresses, usernames, and passwords are stored **locally on your device only** (in the app's private database). They are never sent to us.
- **Personal information**: we do not require an account, email address, or any registration. We do not ask for your name, phone number, or payment details.

## Data Collected With Your Consent

On first launch, the App shows a consent dialog ("Data & Ads Consent"). Your choice is stored on your device and applied immediately.

The App uses **Google AdMob** for advertising where it is available, and falls back to **Yandex Mobile Ads** where AdMob cannot serve (for example, on devices without Google Play Services). Only one advertising network is active at a time.

### If you ACCEPT

The App integrates the following services:

**1. Yandex AppMetrica (analytics)**

Collected to understand app usage and stability:

- Device information (model, OS version, screen size, locale)
- App version and session events (launch, activity screens viewed)
- Crash reports and performance diagnostics
- Advertising identifiers (only if available on your device)
- IP address (in technical logs, for crash diagnostics)

Data is collected by Yandex AppMetrica and processed under the [Yandex AppMetrica Terms](https://yandex.com/legal/appmetrica_termsofuse/).

**2. Advertising — Google AdMob and/or Yandex Mobile Ads**

- The App shows banner ads on the Home screen and an interstitial ad before connecting
- With consent, ads may be personalized using advertising identifiers and collected usage data
- Ad networks and their partners use advertising identifiers (e.g., Google Advertising ID, if available on your device) for ad delivery and frequency capping
- Ad revenue attribution and ad interaction events are reported to AppMetrica
- Google: processing is described in the [Google Privacy & Terms](https://policies.google.com/technologies/partner-sites) and the [Google Ads data disclosure](https://policies.google.com/ads)
- Yandex: processing is described in the [Yandex Mobile Ads Terms and privacy documentation](https://yandex.com/legal/mobileads_agreement/)

### If you DECLINE

- **No analytics data is sent** to AppMetrica (data sending is fully disabled)
- Ads continue to be shown but are **non-personalized (contextual)**: AdMob receives an explicit request to opt out of personalized ads, and Yandex Mobile Ads is set to non-personalized
- Advertising identifiers are not used for personalization

Declining does not restrict any VPN feature.

## Local Data Storage

The following data is stored only on your device and is never transmitted to us:

- Server and subscription configurations (in the app's private database)
- App settings (theme, DNS, routing rules)
- VPN connection state and traffic statistics
- Your consent choice

Uninstalling the App deletes all local data.

## Permissions

- **VPN Service** — required to route your traffic through the tunnel you configure
- **Notifications** — used to show the VPN connection status (Android requirement for foreground services)
- **Camera** — optional, for scanning QR codes to add servers
- **Storage access** — for importing/exporting your configuration

## Push Notifications

The App does not currently send push notifications. If push notifications are added in a future version, they would be delivered through Firebase or a similar provider and would require additional consent.

## Children's Privacy

The App is not directed at children under 13 (or the equivalent minimum age in your jurisdiction). We do not knowingly collect personal data from children.

## Third-Party Servers

The App connects to **proxy servers that you configure**. Your traffic, including destinations, content, and connection metadata, passes through and is governed by those servers and their operators. This may include servers operated by third parties from your subscriptions. We have no control over, and are not responsible for, the data practices of those servers.

## Your Rights and Choices

- **Consent**: You can review the consent dialog by clearing the App's data or reinstalling. All analytics and personalized advertising is disabled unless you accept.
- **Delete data**: Uninstalling the App removes all local data. To exercise data deletion rights for analytics data collected by Yandex, refer to [Yandex's privacy documentation](https://yandex.com/legal/confidential/). For Google, use [Google's Ads Settings](https://adssettings.google.com/) and [My Activity](https://myaccount.google.com/data-and-privacy).
- **Opt out of ad personalization**: You can also reset or delete your Advertising ID in your device Settings (Settings > Privacy > Ads).

## Security

- VPN credentials are stored in the app's private storage, which is isolated by Android's sandbox
- We do not collect or store payment information, as the App is free and has no in-app purchases

## Changes to This Policy

We may update this policy to reflect changes in the App or legal requirements. Updates will be posted at this URL with a revised "Last updated" date. Continued use of the App after changes means you accept the updated policy.

## Contact

For questions about this policy, contact us via the GitHub repository:
https://github.com/majidghafouri/goldenV2

## Legal Notes

- The App is provided for educational purposes. Users are responsible for complying with local laws and regulations regarding proxy/VPN usage.
- Yandex AppMetrica and Yandex Mobile Ads are services of Yandex, governed by their respective terms (linked above).
- Google AdMob is a service of Google LLC, governed by the [Google Privacy & Terms](https://policies.google.com/privacy) and the [AdMob & AdSense policies](https://support.google.com/admob/answer/6128543).
