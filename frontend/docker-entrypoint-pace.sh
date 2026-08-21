#!/bin/sh
set -eu

if [ -z "${PACE_ACCESS_PASSWORD:-}" ] || [ -z "${PACE_API_ACCESS_TOKEN:-}" ]; then
  echo "Les secrets d'accès Pace sont obligatoires." >&2
  exit 1
fi

htpasswd -bc /tmp/pace.htpasswd pace "$PACE_ACCESS_PASSWORD" >/dev/null
envsubst '${PACE_API_ACCESS_TOKEN}' \
  < /etc/pace/pace.conf.template \
  > /etc/nginx/conf.d/pace.conf
