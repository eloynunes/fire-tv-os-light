#!/usr/bin/env bash
# Liga/desliga o TV Light como tela inicial (serviço de acessibilidade). Totalmente reversível.
# Uso: ./scripts/home-lock.sh on|off|status
source "$(dirname "$0")/env.sh"
SVC="$PKG/$PKG.HomeRedirectService"
case "${1:-status}" in
  on)  adbd shell settings put secure enabled_accessibility_services "$SVC"
       adbd shell settings put secure accessibility_enabled 1 ;;
  off) adbd shell settings delete secure enabled_accessibility_services >/dev/null
       adbd shell settings put secure accessibility_enabled 0 ;;
esac
echo "serviços=$(adbd shell settings get secure enabled_accessibility_services) ativo=$(adbd shell settings get secure accessibility_enabled)"
