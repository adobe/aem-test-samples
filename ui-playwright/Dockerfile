FROM mcr.microsoft.com/playwright:v1.56.0-noble

# Install dependencies
RUN apt -y update \
    && apt -y --no-install-recommends install curl libnss3-tools \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user to run the tests
RUN groupadd "runner" && useradd -g "runner" --shell /bin/bash "runner" \
    && mkdir -p "/home/runner" \
    && chown -R "runner":"runner" "/home/runner"

# Set Application Environment
WORKDIR /usr/src/app
COPY ./test-module ./
RUN npm install

ENTRYPOINT ["bash", "run.sh"]
