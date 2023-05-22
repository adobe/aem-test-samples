FROM cypress/included:12.12.0

ENV APP_PATH /usr/src/app
## Install dependencies
RUN apt -qqy update \
    # Generic dependencies
    && apt -qqy --no-install-recommends install \
    python \
    build-essential
# <<< End EAAS Convention \

# Set Application Environment
WORKDIR ${APP_PATH}
COPY ./test-module ./
RUN npm install
# no need to wait for selenium
ENTRYPOINT ["bash","run.sh"]

