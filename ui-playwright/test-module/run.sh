#!/usr/bin/env bash

# setup proxy environment variables and CA certificate
if [ -n "${PROXY_HOST:-}" ]; then
  if [ -n "${PROXY_HTTPS_PORT:-}" ]; then
    export HTTP_PROXY="https://${PROXY_HOST}:${PROXY_HTTPS_PORT}"
  elif [ -n "${PROXY_HTTP_PORT:-}" ]; then
    export HTTP_PROXY="http://${PROXY_HOST}:${PROXY_HTTP_PORT}"
  fi
  if [ -n "${PROXY_CA_PATH:-}" ]; then
    echo "installing certificate"
    mkdir -p $HOME/.pki/nssdb
    certutil -d sql:$HOME/.pki/nssdb -A -t "CT,c,c" -n "EaaS Client Proxy Root" -i $PROXY_CA_PATH
    export NODE_EXTRA_CA_CERTS=${PROXY_CA_PATH}

    # Create policies.json for Firefox to trust the proxy CA
    mkdir -p "$HOME/.mozilla/firefox/playwright-profile"
    echo "{\"policies\":{\"Certificates\":{\"Install\":[\"$PROXY_CA_PATH\"]}}}" > "$HOME/.mozilla/firefox/playwright-profile/policies.json"
    export PLAYWRIGHT_FIREFOX_POLICIES_JSON="$HOME/.mozilla/firefox/playwright-profile/policies.json"

    # add certificate to system trusted certificates for webkit
     cp $PROXY_CA_PATH /usr/local/share/ca-certificates/eaas-proxy.crt
     update-ca-certificates
  fi
  if [ -n "${PROXY_OBSERVABILITY_PORT:-}" ] && [ -n "${HTTP_PROXY:-}" ]; then
    echo "Waiting for proxy"
    curl --silent  --retry ${PROXY_RETRY_ATTEMPTS:-3} --retry-connrefused --retry-delay ${PROXY_RETRY_DELAY:-10} \
      --proxy ${HTTP_PROXY} --proxy-cacert ${PROXY_CA_PATH:-""} \
      ${PROXY_HOST}:${PROXY_OBSERVABILITY_PORT}
    if [ $? -ne 0 ]; then
      echo "Proxy is not ready"
      exit 1
    fi
  fi
fi

# switch user to non-root
su runner

# execute tests
npx playwright test