# Herd toolchain image for Driving Log: what the gate (CLAUDE.md's final regression run) needs, nothing else.
# The herd adds git, the openspec CLI and its worker user on top (its role layer). Worker containers run with a
# read-only root filesystem, so everything the build would otherwise install on demand into the Android SDK or as a
# Gradle-provisioned JDK must already be here.

# The SDK is fetched in its own stage, so the download tools never reach the worker image.
FROM docker.io/library/eclipse-temurin:25-jdk-noble AS sdk
ARG CMDLINE_TOOLS=16111833
ENV ANDROID_HOME=/opt/android-sdk
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
 && mkdir -p "$ANDROID_HOME/cmdline-tools" \
 && curl -fsSLo /tmp/tools.zip \
      "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip" \
 && unzip -q /tmp/tools.zip -d "$ANDROID_HOME/cmdline-tools" \
 && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
# compileSdk 37 (gradle/libs.versions.toml), the build-tools AGP picks by default, and platform-tools, which AGP
# otherwise tries to install on every build. Bump these with AGP or compileSdk: in a worker the SDK is read-only, so
# a missing one fails the gate instead of being downloaded.
RUN yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null \
 && "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-37.0" "build-tools;36.0.0" "platform-tools"

FROM docker.io/library/eclipse-temurin:25-jdk-noble
# Ubuntu 24.04 ships a `ubuntu` user with UID 1000, the herd's worker UID. Nothing here needs it.
RUN userdel --remove ubuntu
ENV ANDROID_HOME=/opt/android-sdk
COPY --from=sdk /opt/android-sdk /opt/android-sdk
