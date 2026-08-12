#!/usr/bin/env bash
#
# End-to-end validation for the AndroidNative Unreal Engine plugin.
#
# The full plugin can only be compiled inside a licensed Unreal Engine project
# (the C++ needs the engine source and generated headers). What this repo can
# build and verify standalone is its Android-facing surface:
#
#   1. DeviceInfo.java compiles against the Android SDK + the androidx/guava
#      dependencies declared in AndroidNative_UPL_Android.xml.
#   2. The compiled class dexes into a valid Android classes.dex (this is the
#      artifact Unreal ultimately packages into the .apk).
#   3. The UPL Android XML is well-formed.
#   4. The .uplugin descriptor is valid JSON with the expected module.
#
# Requires the toolchain installed by .cursor/install.sh.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
DEPS_CACHE="${ANDROIDNATIVE_DEPS:-$HOME/.androidnative-deps}"

ANDROID_PLATFORM_DIR="$ANDROID_SDK_ROOT/platforms/android-34"
BUILD_TOOLS_DIR="$ANDROID_SDK_ROOT/build-tools/34.0.0"
ANDROID_JAR="$ANDROID_PLATFORM_DIR/android.jar"

ANNOTATION_JAR="$DEPS_CACHE/annotation-jvm-1.7.1.jar"
CORE_JAR="$DEPS_CACHE/core-1.12.0-classes.jar"
GUAVA_JAR="$DEPS_CACHE/guava-28.2-android.jar"
KOTLIN_JAR="$DEPS_CACHE/kotlin-stdlib-1.9.22.jar"

JAVA_SRC="$REPO_ROOT/Source/AndroidNative/Private/Java/DeviceInfo.java"
UPL_XML="$REPO_ROOT/Source/AndroidNative/AndroidNative_UPL_Android.xml"
UPLUGIN="$REPO_ROOT/AndroidNative.uplugin"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

pass() { printf '  \033[32mPASS\033[0m  %s\n' "$*"; }
fail() { printf '  \033[31mFAIL\033[0m  %s\n' "$*"; exit 1; }
step() { printf '\n== %s ==\n' "$*"; }

require() { [[ -e "$1" ]] || fail "missing required file: $1  (run .cursor/install.sh first)"; }

step "Toolchain"
command -v javac >/dev/null || fail "javac not found"
require "$ANDROID_JAR"
require "$BUILD_TOOLS_DIR/d8"
require "$ANNOTATION_JAR"; require "$CORE_JAR"; require "$GUAVA_JAR"; require "$KOTLIN_JAR"
pass "JDK: $(javac -version 2>&1)"
pass "Android platform: $ANDROID_JAR"
pass "Build-tools: $BUILD_TOOLS_DIR"

step "1/4 Compile DeviceInfo.java against Android + declared dependencies"
CP="$ANDROID_JAR:$ANNOTATION_JAR:$CORE_JAR:$GUAVA_JAR:$KOTLIN_JAR"
mkdir -p "$WORK/classes"
javac -Xlint:none -nowarn -d "$WORK/classes" -classpath "$CP" \
      -source 17 -target 17 "$JAVA_SRC" 2> "$WORK/javac.log" || {
  cat "$WORK/javac.log"; fail "javac failed to compile DeviceInfo.java"; }
CLASS="$WORK/classes/com/Plugins/AndroidNative/DeviceInfo.class"
[[ -f "$CLASS" ]] || fail "expected class file was not produced"
pass "compiled -> com/Plugins/AndroidNative/DeviceInfo.class"

# The C++ side (StaticNativeCaller) invokes these methods over JNI, so verify
# the compiled class exposes exactly the public API the plugin relies on.
EXPECTED_METHODS="GetGeoLocation IsInternetAvailable GetCurrentSystemTheme \
GetExternalPath GetUniqueID GetOSVersion GetSDKVersion GetBrand GetModel \
GetProduct GetLanguage GetLanguageCode"
API="$(javap -classpath "$WORK/classes" com.Plugins.AndroidNative.DeviceInfo)"
for m in $EXPECTED_METHODS; do
  grep -q " $m(" <<<"$API" || fail "compiled class is missing expected JNI method: $m"
done
pass "all expected JNI methods present ($(wc -w <<<"$EXPECTED_METHODS") methods)"

step "2/4 Dex the compiled class into an Android classes.dex"
# Dex every produced .class (DeviceInfo plus any nested/anonymous classes such
# as the NSD RegistrationListener), since d8 needs nest mates on the class path.
mapfile -t ALL_CLASSES < <(find "$WORK/classes" -name '*.class')
"$BUILD_TOOLS_DIR/d8" --output "$WORK" --lib "$ANDROID_JAR" "${ALL_CLASSES[@]}" 2> "$WORK/d8.log" || {
  cat "$WORK/d8.log"; fail "d8 failed to dex the compiled classes"; }
[[ -f "$WORK/classes.dex" ]] || fail "d8 did not produce classes.dex"
DESC="$("$BUILD_TOOLS_DIR/dexdump" "$WORK/classes.dex" | grep -c "Class descriptor  : 'Lcom/Plugins/AndroidNative/DeviceInfo;'")"
[[ "$DESC" == "1" ]] || fail "classes.dex does not contain the expected class descriptor"
pass "produced valid classes.dex ($(stat -c%s "$WORK/classes.dex") bytes) containing DeviceInfo"

step "3/4 Validate UPL Android XML is well-formed"
require "$UPL_XML"
python3 -c "import sys, xml.dom.minidom as m; m.parse(sys.argv[1])" "$UPL_XML" \
  || fail "AndroidNative_UPL_Android.xml is not well-formed XML"
pass "AndroidNative_UPL_Android.xml is well-formed"

step "4/4 Validate .uplugin descriptor JSON"
require "$UPLUGIN"
python3 - "$UPLUGIN" <<'PY' || fail ".uplugin is not valid / missing expected module"
import json, sys
with open(sys.argv[1]) as f:
    d = json.load(f)
mods = [m.get("Name") for m in d.get("Modules", [])]
assert "AndroidNative" in mods, f"AndroidNative module not found in {mods}"
print("  module(s):", ", ".join(mods))
PY
pass "AndroidNative.uplugin is valid JSON with the AndroidNative module"

printf '\n\033[32mAll validation checks passed.\033[0m\n'
