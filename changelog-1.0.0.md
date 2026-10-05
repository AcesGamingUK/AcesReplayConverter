# Aces Replay Converter 1.0.0

First Aces 1.0 release, based on the working 0.1.4-dev build.

- Native Minecraft 1.21.1 / protocol 767 replay conversion.
- Native conversion bypasses ViaVersion startup and configuration loading.
- Correct JVM verification for the packet passthrough method.
- Snapshot packet order is preserved without the older equipment-packet merge.
- Existing recorded Simple Voice Chat integration is retained.
- New project icon and release version metadata.

Conversion code and bundled dependencies are unchanged from 0.1.4-dev. That build passed strict Java 21 JVM and ASM checks, completed conversion of a 257,694-packet test recording, and was reported working in playback by the tester.

Targets Minecraft 1.21.1 and was tested with Flashback 0.39.10. Older modpack recordings and changed mod packet formats may be incompatible. NeoForge registry restoration is not included in this JAR.
