#!/usr/bin/env bash

# Copyright 2023 Adobe Systems Incorporated
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

# start a single X11 server and pass the server's address to each Cypress instance using DISPLAY variable
pkill Xvfb
echo 'start xvfb'
Xvfb :99 -screen 0 1280x1024x24 -ac -nolisten tcp -nolisten unix &
export DISPLAY=:99
echo 'checking Xvfb'
ps aux | grep Xvfb
# disable color output when running Cypress
export NO_COLOR=1
#export ELECTRON_EXTRA_LAUNCH_ARGS=--remote-debugging-port=9222

# setup proxy environment variables
if [ -n "${PROXY_HOST:-}" ]; then
  if [ -n "${PROXY_HTTPS_PORT:-}" ]; then
      export HTTP_PROXY="https://${PROXY_HOST}:${PROXY_HTTPS_PORT}"
      export NODE_EXTRA_CA_CERTS=${PROXY_CA_PATH}
  elif [ -n "${PROXY_HTTP_PORT:-}" ]; then
    export HTTP_PROXY="http://${PROXY_HOST}:${PROXY_HTTP_PORT}"
  fi
  if [ -n "${PROXY_OBSERVABILITY_PORT:-}" ]; then
    echo "Waiting for proxy"
    curl --silent --retry ${PROXY_RETRY_ATTEMPTS:-3} --retry-connrefused --retry-delay ${PROXY_RETRY_DELAY:-10} \
      ${PROXY_HOST}:${PROXY_OBSERVABILITY_PORT}
  fi
fi

# execute tests
npm test
