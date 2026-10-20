#!/usr/bin/env bash
# Собирает подписанный APK и выкладывает его на сервер: https://racing.ilyaushenin.ru/download/nn-racing.apk
set -euo pipefail
cd "$(dirname "$0")"
SSH_TARGET=${RACING_SSH:-ubuntu@92.5.35.104}
KEY=${RACING_KEY:-$HOME/ssh-key-2026-08-25-2.key}
./gradlew :composeApp:assembleRelease -q
APK=composeApp/build/outputs/apk/release/composeApp-release.apk
scp -i "$KEY" -q "$APK" "$SSH_TARGET:/tmp/nn-racing.apk"
ssh -i "$KEY" "$SSH_TARGET" 'sudo install -d -m 755 /var/lib/racing/downloads && sudo install -m 644 -o root -g root /tmp/nn-racing.apk /var/lib/racing/downloads/nn-racing.apk && rm /tmp/nn-racing.apk'
echo "== готово: https://racing.ilyaushenin.ru/download/nn-racing.apk"
