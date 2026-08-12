#!/usr/bin/env bash
#
# Cloud Agent install script for the AndroidNative Unreal Engine plugin.
#
# This plugin is an Unreal Engine module: its C++ depends on the Unreal Engine
# source tree (Core/CoreUObject/Engine, generated headers, UE JNI types) which
# is not redistributable and cannot be compiled without a licensed engine.
# The independently buildable artifact shipped in this repo is the Android Java
# source (Source/AndroidNative/Private/Java/DeviceInfo.java) that Unreal's UPL
# pipeline (AndroidNative_UPL_Android.xml) compiles against the Android SDK and
# the androidx/guava dependencies it declares.
#
# This script installs the toolchain needed to compile and dex that Java code:
#   - Android SDK command-line tools, a platform (android.jar) and build-tools
#   - The androidx / guava dependencies declared by the UPL, cached locally
#
# It is idempotent: re-running it re-uses anything already installed.
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
DEPS_CACHE="${ANDROIDNATIVE_DEPS:-$HOME/.androidnative-deps}"

CMDLINE_TOOLS_VERSION="11076708"
ANDROID_PLATFORM="platforms;android-34"
ANDROID_BUILD_TOOLS="build-tools;34.0.0"

# Dependency coordinates (pinned) declared/used by the plugin's Android build.
ANNOTATION_JVM_VER="1.7.1"
CORE_VER="1.12.0"
GUAVA_VER="28.2-android"      # declared in AndroidNative_UPL_Android.xml
KOTLIN_STDLIB_VER="1.9.22"    # androidx annotations carry kotlin metadata

GMAVEN="https://dl.google.com/dl/android/maven2"
MAVEN="https://repo1.maven.org/maven2"

log() { printf '[install] %s\n' "$*"; }

fetch() {
  # fetch <url> <dest>  (skips if already present)
  local url="$1" dest="$2"
  if [[ -f "$dest" ]]; then
    log "cached: $(basename "$dest")"
    return 0
  fi
  log "downloading: $(basename "$dest")"
  curl -fsSL --retry 4 --retry-delay 2 -o "$dest.tmp" "$url"
  mv "$dest.tmp" "$dest"
}

install_cmdline_tools() {
  local sdkm="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  if [[ -x "$sdkm" ]]; then
    log "cmdline-tools already installed"
    return 0
  fi
  log "installing Android command-line tools"
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  local zip="/tmp/android-cmdline-tools-${CMDLINE_TOOLS_VERSION}.zip"
  fetch "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" "$zip"
  local tmp
  tmp="$(mktemp -d)"
  unzip -q "$zip" -d "$tmp"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
}

install_sdk_packages() {
  local sdkm="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  # Accept licenses (no-op once accepted) then install packages (no-op if present).
  yes | "$sdkm" --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null 2>&1 || true
  log "ensuring SDK packages: platform-tools $ANDROID_PLATFORM $ANDROID_BUILD_TOOLS"
  "$sdkm" --sdk_root="$ANDROID_SDK_ROOT" \
    "platform-tools" "$ANDROID_PLATFORM" "$ANDROID_BUILD_TOOLS" >/dev/null
}

prefetch_dependencies() {
  mkdir -p "$DEPS_CACHE"
  fetch "$GMAVEN/androidx/annotation/annotation-jvm/${ANNOTATION_JVM_VER}/annotation-jvm-${ANNOTATION_JVM_VER}.jar" \
        "$DEPS_CACHE/annotation-jvm-${ANNOTATION_JVM_VER}.jar"
  fetch "$GMAVEN/androidx/core/core/${CORE_VER}/core-${CORE_VER}.aar" \
        "$DEPS_CACHE/core-${CORE_VER}.aar"
  fetch "$MAVEN/com/google/guava/guava/${GUAVA_VER}/guava-${GUAVA_VER}.jar" \
        "$DEPS_CACHE/guava-${GUAVA_VER}.jar"
  fetch "$MAVEN/org/jetbrains/kotlin/kotlin-stdlib/${KOTLIN_STDLIB_VER}/kotlin-stdlib-${KOTLIN_STDLIB_VER}.jar" \
        "$DEPS_CACHE/kotlin-stdlib-${KOTLIN_STDLIB_VER}.jar"

  # androidx.core ships as an .aar; extract its classes.jar for the javac classpath.
  local core_classes="$DEPS_CACHE/core-${CORE_VER}-classes.jar"
  if [[ ! -f "$core_classes" ]]; then
    log "extracting classes.jar from androidx.core aar"
    local tmp
    tmp="$(mktemp -d)"
    unzip -q -o "$DEPS_CACHE/core-${CORE_VER}.aar" classes.jar -d "$tmp"
    mv "$tmp/classes.jar" "$core_classes"
    rm -rf "$tmp"
  fi
}

main() {
  command -v java >/dev/null || { echo "ERROR: java (JDK) is required but not found" >&2; exit 1; }
  command -v unzip >/dev/null || { echo "ERROR: unzip is required but not found" >&2; exit 1; }
  command -v curl >/dev/null || { echo "ERROR: curl is required but not found" >&2; exit 1; }

  install_cmdline_tools
  install_sdk_packages
  prefetch_dependencies

  log "done. ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT  deps=$DEPS_CACHE"
}

main "$@"
