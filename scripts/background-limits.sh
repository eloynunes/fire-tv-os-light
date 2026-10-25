#!/usr/bin/env bash
# Impede apps de streaming de rodarem sozinhos em segundo plano (appop RUN_ANY_IN_BACKGROUND).
# Abrir o app funciona normalmente. Reversível: ./scripts/background-limits.sh revert
source "$(dirname "$0")/env.sh"
# Spotify fora (toca música em segundo plano). Serviços de sistema da Amazon fora.
APPS="com.amazon.firebat com.netflix.ninja com.amazon.firetv.youtube com.disney.disneyplus com.disney.starplus
com.globo.globotv com.hbo.hbonow br.telecine.androidtv com.apple.atve.amazon.appletv com.brasilparalelo.athena
com.cbs.ca com.crunchyroll.crunchyroid com.dazn com.facebook.katana com.foxxum.onefootball.firetv
com.maxximos.xtt com.ted.android.tv com.tid.enjoy.firetv com.uefa.uefatv com.viki.kindle com.vix.fire.vixtv
tv.twitch.android.viewer com.deepakpk.tvexplorer"
MODE=ignore; [[ "${1:-apply}" == "revert" ]] && MODE=allow
for p in $APPS; do
  adbd shell pm path "$p" >/dev/null 2>&1 || continue
  adbd shell cmd appops set "$p" RUN_ANY_IN_BACKGROUND $MODE
  [[ $MODE == ignore ]] && adbd shell am kill "$p" >/dev/null 2>&1
  printf "%s=%s " "$p" "$(adbd shell cmd appops get "$p" RUN_ANY_IN_BACKGROUND | tr -d '\r' | sed 's/RUN_ANY_IN_BACKGROUND: //;s/;.*//')"
done; echo
