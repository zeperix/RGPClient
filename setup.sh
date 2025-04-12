#!/bin/bash
set -e

# === 🧩 Cấu hình thư mục ===
SDK_DIR="$(pwd)/Android/sdk"
CMDLINE_DIR="$SDK_DIR/cmdline-tools/latest"
CMDLINE_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_ZIP="$SDK_DIR/cmdline-tools.zip"

BUILD_TOOLS_VERSION="34.0.0"
PLATFORM_VERSION="android-34"

# === ☕ Ưu tiên Java 17 ===
JAVA17_PATH="/usr/lib/jvm/java-17-openjdk-amd64"
export JAVA_HOME="$JAVA17_PATH"
export PATH="$JAVA_HOME/bin:$PATH"

echo "☕ Sử dụng Java version:"
java -version

# === 📁 Tạo thư mục SDK ===
mkdir -p "$CMDLINE_DIR"

# === 📦 Tải về command line tools ===
echo "📦 Đang tải command line tools..."
wget -q "$CMDLINE_ZIP_URL" -O "$CMDLINE_ZIP"

# === 📂 Giải nén ===
echo "📂 Đang giải nén..."
unzip -q "$CMDLINE_ZIP" -d "$SDK_DIR/tmp-tools"
rsync -a "$SDK_DIR/tmp-tools/cmdline-tools/" "$CMDLINE_DIR/"
rm -rf "$SDK_DIR/tmp-tools"
rm -f "$CMDLINE_ZIP"

# === 🧰 Thêm SDK vào PATH ===
export ANDROID_SDK_ROOT="$SDK_DIR"
export PATH="$CMDLINE_DIR/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# === ⚙️ Cài thành phần cần thiết ===
echo "✅ Đã sẵn sàng cài đặt Android SDK..."

yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "build-tools;$BUILD_TOOLS_VERSION" \
  "platforms;$PLATFORM_VERSION"

echo "🎉 Android SDK đã được cài tại: $ANDROID_SDK_ROOT"
echo "org.gradle.java.home=$JAVA_HOME" > gradle.properties
echo "🛠️ Sẵn sáng để xây dựng ứng dụng"
