# Aces Replay Converter

Convert Minecraft 1.21.1 ReplayMod recordings into Flashback replays from inside the game, with support for recorded Simple Voice Chat data.

This repository hosts the Aces release documentation, local patch sources, and issue tracker.

## Report a problem

[Open a bug report](https://github.com/AcesGamingUK/AcesReplayConverter/issues/new/choose).

Include your converter version, Flashback version, mod loader, recording Minecraft version, and relevant log or crash report. For modded recordings, tell us whether the recording was made with the same modpack version used for playback.

## Requirements

- Minecraft **1.21.1**, Java **21+**.
- Fabric Loader **0.18.4+**, Fabric API, and Flashback.
- Tested with Flashback **0.39.10**. The JAR declares Flashback **0.39.5–0.39.10**.
- Simple Voice Chat for supported recorded voice playback.

The artifact is a **Fabric JAR**. It has been used successfully on NeoForge through **Sinytra Connector**, but it is not a native NeoForge build.

## Use

1. Install the converter alongside Flashback and its dependencies.
2. Open Flashback's replay selection screen.
3. Click **Convert from replay mcpr**.
4. Select a Minecraft 1.21.1 `.mcpr` recording and confirm conversion.
5. Open the converted `.zip` replay in Flashback.

## Compatibility

Version 1.0.0 is based on the working 0.1.4-dev build and targets native protocol **767** recordings. Conversion from other Minecraft versions is not supported by this Aces release.

Recordings from older modpack versions can fail during playback because mod packet formats and registry mappings differ. Conversion success alone does not guarantee playback compatibility. This release does not restore omitted NeoForge registry snapshots automatically.

Only audio present in the source recording can be converted.

## Release 1.0.0

- Native 767 → 767 conversion bypasses ViaVersion startup and configuration loading.
- Rebuilt packet passthrough method with valid JVM verification.
- Native snapshot packet order is preserved without the older equipment-packet merge.
- Recorded Simple Voice Chat integration retained.
- New Aces project image and release metadata.

The conversion code is identical to 0.1.4-dev. That build passed Java 21 JVM and ASM verification, converted a 257,694-packet test recording, and was reported working during playback.

## Credits and source status

Based on [Flashback Converter by Byzantonia](https://github.com/Byzantonia/replay-to-flashback-converter), with the Aces native 1.21.1 fixes maintained by **AcesGamingUK**.

The local patch sources are repair and verification utilities, not a complete standalone Gradle source tree. The linked Byzantonia repository is the upstream project; it does not contain the Aces-specific release changes.

The distributed mod is **GPL-3.0-only**, with upstream credits and third-party notices retained. See the bundled license and notice files.
