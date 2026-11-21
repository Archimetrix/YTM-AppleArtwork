# YTM-AppleArtwork

Apple Music **animated artwork** (motion covers) on top of the thumbnail in the YouTube Music
player, as a separate [Morphe](https://github.com/MorpheApp) patch bundle.

**🌫️This patch is now included in the morphe official patch no need to install this patch seperately now just normally patch with morphe patches and you will get the feature**

- Fetches artwork **directly from Apple** (no third-party server).
- Handles Apple's HLS (`.m3u8`) by assembling the clip into a local file and looping it.
- Settings entry in YouTube Music: **on/off**, a **persistent log** (copy / share / clear) and a cache cleaner.
- Works with root and non-root installs.
- Songs without motion artwork are left untouched.

## Install

1. Morphe Manager -> **Sources** -> add this repo link :
   `https://github.com/Archimetrix/YTM-AppleArtwork` or press this morphe button .
   [![Add to Morphe](https://img.shields.io/badge/Add%20to-Morphe-6c5ce7?style=for-the-badge&logo=android&logoColor=white)](https://morphe.software/add-source?github=Archimetrix/YTM-AppleArtwork)
3. Patch YouTube Music with **Morphe Patches** and this bundle selected together.
4. Updates show up in Manager like any other source.

Supported YouTube Music versions follow Morphe's list (9.15.51 - 9.46.51; the newest ones are experimental).

## Settings

YouTube Music -> Settings -> **Apple Music artwork**.

## Troubleshooting

Open the settings entry and press **Copy** on the log, then open an issue with it.
If YouTube Music crashes, a "crashed last time" popup with the stack trace appears on the next launch.

## Build

See `scripts/build.sh` (needs JDK, kotlinc, dx, android.jar and the Morphe patcher jar):

```
PATCHER_JAR=morphe-desktop-2.2.0-all.jar scripts/build.sh 1.2.1
```

## License

AGPL-3.0-or-later. See `LICENSE` and `NOTICE`.

Built with love by [Archimetrix](https://github.com/Archimetrix).
