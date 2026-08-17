# iOS host

This directory contains the minimal SwiftUI host and entitlements for the shared Compose Multiplatform framework.

Create an Xcode iOS App target that includes:

- `AdzanNotifApp.swift` and `Info.plist`;
- the `AdzanNotifShared` framework produced by the `shared` module;
- `AdzanNotif.entitlements` with the `group.com.adzannotif.app` App Group;
- the `AdzanNotifWidgets` extension from this directory.

The host delegates prayer calculation and offline snapshot persistence to `shared/src/iosMain`. The local notification adapter requests authorization before scheduling the next real computed prayer. No static schedule is bundled into the host.
