FROM mcr.microsoft.com/playwright:v1.48.1-jammy

RUN apt -y update \
    && apt -y --no-install-recommends install curl libnss3-tools \
    && rm -rf /var/lib/apt/lists/*

# Set Application Environment
WORKDIR /usr/src/app
COPY ./test-module ./
RUN npm install

ENTRYPOINT ["bash","run.sh"]