# Releasing an update (so Morphe Manager shows it)

Morphe Manager reads `patches-bundle.json` from the **root of the `main` branch** and compares
`version` / `created_at` with what the user has installed.

1. Bump the version: `CHANGELOG.md`, and build with it: `scripts/build.sh X.Y.Z` (-> `dist/apple-music-artwork-X.Y.Z.mpp`).
2. GitHub -> Releases -> **Draft a new release**, tag `vX.Y.Z`, put the changelog in the description,
   **attach the .mpp file before pressing Publish**.
3. Publishing triggers `.github/workflows/update-bundle.yml`, which rewrites `patches-bundle.json`
   (version, created_at, description, download_url) and commits it to `main`.
   (No Actions? Edit `patches-bundle.json` by hand, see the example below.)
4. Check `https://raw.githubusercontent.com/Archimetrix/YTM-AppleArtwork/main/patches-bundle.json`
   shows the new version. Users then get the update in Morphe Manager.

Rules: the repo must be **public**; `version` must be higher than the previous one; `created_at`
is UTC without a timezone suffix; `download_url` must be a direct link to the `.mpp`.

```json
{
  "created_at": "2026-11-20T10:00:00",
  "description": "- what changed",
  "download_url": "https://github.com/Archimetrix/YTM-AppleArtwork/releases/download/v1.2.1/apple-music-artwork-1.2.1.mpp",
  "version": "1.2.1"
}
```
