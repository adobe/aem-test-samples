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
FROM maven:3.8.6-eclipse-temurin-11

# Java options playing well with k8s
ENV JAVA_OPTIONS="-server -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2 -XX:+UseG1GC -XshowSettings:vm $JAVA_OPTIONS"

ENV APP_PATH /usr/src/app
ENV SELENIUM_STARTUP_TIMEOUT 60

# Begin CloudManager Convention >>>
ENV SELENIUM_BASE_URL http://localhost:4444
ENV SELENIUM_BROWSER chrome
ENV REPORTS_PATH reports
ENV AEM_AUTHOR_URL http://localhost:4502
ENV AEM_AUTHOR_USERNAME admin
ENV AEM_AUTHOR_PASSWORD admin
ENV AEM_PUBLISH_URL http://localhost:4503
ENV AEM_PUBLISH_USERNAME admin
ENV AEM_PUBLISH_PASSWORD admin

# Following convention variables are null by default
# ENV SHARED_FOLDER
# ENV UPLOAD_URL

# <<< End CloudManager Convention

# Set Application Environment
WORKDIR ${APP_PATH}

# Copy project
ENV TEST_RUN_COMMAND "mvn verify -Pui-tests-cloud-execution"
COPY test-module ./

# download all Maven dependency into image to prevent download / timeout during each execution
RUN mvn --batch-mode dependency:resolve-plugins dependency:go-offline install -DskipTests -Dassembly.skipAssembly=true
RUN mvn --batch-mode dependency:resolve-plugins dependency:go-offline verify -Pui-tests-local-execution -DskipTests -Dassembly.skipAssembly=true

# Begin CloudManager Convention >>>
COPY wait-for-grid.sh .
RUN chmod 555 wait-for-grid.sh

CMD ./wait-for-grid.sh ${TEST_RUN_COMMAND}
# <<< End CloudManager Convention
