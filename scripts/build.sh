#!/usr/bin/env bash
# Builds dist/apple-music-artwork-<version>.mpp
#
# Needs: JDK 17+ (javac), kotlinc 2.x, dx (dalvik-exchange), an android.jar, and the Morphe
# patcher API (the morphe-desktop "-all.jar" release contains it).
#   Ubuntu: sudo apt install openjdk-21-jdk-headless dalvik-exchange libandroid-23-java
#   Kotlin: https://github.com/JetBrains/kotlin/releases  (kotlin-compiler-*.zip)
#   Patcher: https://github.com/MorpheApp/morphe-desktop/releases (morphe-desktop-*-all.jar)
#
# usage: PATCHER_JAR=/path/morphe-desktop-2.2.0-all.jar scripts/build.sh 1.2.1
set -euo pipefail
VERSION="${1:?usage: build.sh <version, e.g. 1.2.1>}"
PATCHER_JAR="${PATCHER_JAR:?set PATCHER_JAR to morphe-desktop-*-all.jar}"
ANDROID_JAR="${ANDROID_JAR:-/usr/share/java/com.android.android-23.jar}"
DX_JAR="${DX_JAR:-/usr/share/java/com.android.dx.jar}"
KOTLINC="${KOTLINC:-kotlinc}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
W="$ROOT/build"; OUT="$ROOT/dist"
rm -rf "$W"; mkdir -p "$W"/{ext_cls,patch_cls,ext_dex,patch_dex,stage/META-INF,stage/extensions} "$OUT"
dx() { java -cp "$DX_JAR" com.android.dx.command.Main "$@"; }

echo "[1/5] extension (Java -> dex)"
javac --release 8 -cp "$ANDROID_JAR" -d "$W/ext_cls" "$ROOT"/extension/src/dev/archimetrix/applemusic/*.java
dx --dex --min-sdk-version=26 --output="$W/ext_dex/classes.dex" "$W/ext_cls"

echo "[2/5] patch (Kotlin)"
"$KOTLINC" -jvm-target 11 -Xlambdas=class -Xsam-conversions=class -no-reflect \
  -cp "$PATCHER_JAR" "$ROOT"/patches/src/dev/archimetrix/patches/applemusic/*.kt -d "$W/patch_cls"

echo "[3/5] patch classes -> classes.dex (Android Manager loads this)"
dx --dex --min-sdk-version=26 --no-strict --output="$W/patch_dex/classes.dex" "$W/patch_cls"

echo "[4/5] assemble .mpp"
S="$W/stage"
cp -r "$W/patch_cls/dev" "$S/"
cp "$W"/patch_cls/META-INF/*.kotlin_module "$S/META-INF/" 2>/dev/null || true
cp "$W/patch_dex/classes.dex" "$S/classes.dex"
cp "$W/ext_dex/classes.dex" "$S/extensions/apple-music-artwork.mpe"
cat > "$S/META-INF/MANIFEST.MF" <<MF
Manifest-Version: 1.0
Name: Apple Music Artwork Patches
Description: YouTube Music - Apple Music animated artwork over the player thumbnail
Version: $VERSION
Timestamp: $(date +%s)000
Author: Archimetrix
Contact: https://github.com/Archimetrix
Website: https://github.com/Archimetrix/YTM-AppleArtwork
License: GNU Affero General Public License v3.0 or later
Patcher-Version: 2.4.0

MF
MPP="$OUT/apple-music-artwork-$VERSION.mpp"
rm -f "$MPP"
( cd "$S" && zip -q -r -X "$MPP" META-INF dev classes.dex extensions )

echo "[5/5] sanity: load with the real patcher"
cat > "$W/Check.java" <<'JV'
import app.morphe.patcher.patch.*; import java.io.File; import java.util.*;
public class Check { public static void main(String[] a) throws Exception {
  int n = 0; for (Patch p : new PatchLoader.Jar(Set.of(new File(a[0])))) { System.out.println("patch: " + p.getName()); n++; }
  if (n == 0) { System.err.println("NO PATCHES FOUND"); System.exit(1); } } }
JV
javac -cp "$PATCHER_JAR" -d "$W" "$W/Check.java"
java -cp "$PATCHER_JAR:$W" Check "$MPP"
echo "OK -> $MPP"
