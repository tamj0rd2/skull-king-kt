#!/usr/bin/env bash

set -eo pipefail

# Installs chrome
VERSION="$(curl https://chromedriver.storage.googleapis.com/LATEST_RELEASE)"
CHROME_VERSION_SHORT="$(echo $VERSION | cut -d. -f1)"
CHROME_ZIPNAME="chrome-$CHROME_VERSION_SHORT.zip"
CHROME_DESTINATION="./.chrome/chrome-$CHROME_VERSION_SHORT.app"
# found this link here - https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json
echo "Downloading chrome $VERSION"
curl --no-progress-meter "https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/$VERSION/mac-arm64/chrome-mac-arm64.zip" --output "$CHROME_ZIPNAME"
unzip -q -o "$CHROME_ZIPNAME"
mkdir -p ".chrome"
rm -rf "$CHROME_DESTINATION"
mv "./chrome-mac-arm64/Google Chrome for Testing.app" "$CHROME_DESTINATION"
rm -rf "./chrome-mac-arm64"
rm "$CHROME_ZIPNAME"
echo "Installed chrome $VERSION to $CHROME_DESTINATION"

# Installs chromedriver
CHROME_DRIVER_ZIPNAME="chromedriver-$CHROME_VERSION_SHORT.zip"
echo "Downloading chromedriver $VERSION"
curl --no-progress-meter "https://chromedriver.storage.googleapis.com/$VERSION/chromedriver_mac_arm64.zip" --output $CHROME_DRIVER_ZIPNAME
mkdir -p ./.chromedriver
unzip -q -o "$CHROME_DRIVER_ZIPNAME" -d ./.chromedriver
mv .chromedriver/chromedriver ./.chromedriver/chromedriver-$CHROME_VERSION_SHORT
rm "$CHROME_DRIVER_ZIPNAME"
echo "Installed chromedriver $VERSION to ./.chromedriver/chromedriver-$CHROME_VERSION_SHORT"

# Updates the chrome and selenium versions being used across the codebase
go run ./update-chrome-version.go "$CHROME_VERSION_SHORT"
