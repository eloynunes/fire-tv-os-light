#!/usr/bin/env bash
# Conecta ao Fire Stick por ADB (TCP). Uso: DEVICE=ip:porta ./scripts/connect.sh
source "$(dirname "$0")/env.sh"
HOST="${DEVICE%%:*}"
nc -z -G 3 "$HOST" "${DEVICE##*:}" 2>/dev/null || { echo "Porta ADB fechada em $DEVICE: ative Depuração ADB no Fire TV."; exit 3; }
adb connect "$DEVICE" | tail -1
STATE=$(adb -s "$DEVICE" get-state 2>&1)
[[ "$STATE" == "device" ]] && echo "ADB OK: $DEVICE" && exit 0
echo "Estado: $STATE — aceite o pedido 'Permitir depuração USB' na TV e rode de novo."; exit 4
