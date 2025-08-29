#!/bin/bash

# Copyright 2022 Adobe Systems Incorporated
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# DO NOT MODIFY
#

# wait-for-grid.sh
set -e

# setup proxy environment variables and CA certificate
MVN_PROXY_PARAMS=

if [ -n "${PROXY_HOST:-}" ] && ( [ -n "${PROXY_HTTP_PORT:-}" ] || [ -n "${PROXY_HTTPS_PORT:-}" ] ); then
  if [ -n "${PROXY_HTTPS_PORT:-}" ]; then
    export HTTP_PROXY="https://${PROXY_HOST}:${PROXY_HTTPS_PORT}"
  elif [ -n "${PROXY_HTTP_PORT:-}" ]; then
    export HTTP_PROXY="http://${PROXY_HOST}:${PROXY_HTTP_PORT}"
    export PROXY_HTTPS_PORT="${PROXY_HTTP_PORT}"
  fi
  if [ -n "${PROXY_CA_PATH:-}" ]; then
    echo "Installing certificate"

    # Add SSL certificate to Java Trust Store to allow SSL termination in the proxy
    cp $JAVA_HOME/lib/security/cacerts ./eaas.jks  # copy the standard trust store from the JRE
    $JAVA_HOME/bin/keytool -import -storepass changeit -trustcacerts -alias eaas-client-proxy -file $PROXY_CA_PATH -keystore eaas.jks -noprompt
    MVN_PROXY_PARAMS=-Djavax.net.ssl.trustStore=eaas.jks
  fi
  if [ -n "${PROXY_OBSERVABILITY_PORT:-}" ] && [ -n "${HTTP_PROXY:-}" ]; then
    echo "Waiting for proxy..."
    printf "Status: " && curl --silent --retry ${PROXY_RETRY_ATTEMPTS:-3} --retry-connrefused --retry-delay ${PROXY_RETRY_DELAY:-10} \
      --proxy ${HTTP_PROXY} --proxy-cacert ${PROXY_CA_PATH:-""} \
      ${PROXY_HOST}:${PROXY_OBSERVABILITY_PORT}
    if [ $? -ne 0 ]; then
      echo "Proxy is not ready"
      exit 1
    fi
  fi
  printf "\nProxy configuration has been completed\n\n"
fi

# Remove trailing slash
SELENIUM_BASE_URL=${SELENIUM_BASE_URL%/}

while ! (curl -sSL "${SELENIUM_BASE_URL}/wd/hub/status" 2>&1 \
        | jq -r '.value.ready' 2>&1 | grep "true" >/dev/null) && [[ "$SECONDS" -lt ${SELENIUM_STARTUP_TIMEOUT} ]]; do
    echo 'Waiting for the Grid'
    sleep 1
done

>&2 echo "Selenium Grid is up - executing tests"

set -o xtrace

mvn --batch-mode \
  verify \
  -Pui-tests-cloud-execution \
  -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS \
  $MVN_PROXY_PARAMS
