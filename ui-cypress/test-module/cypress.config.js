/*
 *  Copyright 2023 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

const { defineConfig } = require("cypress");
const { unlinkSync } = require("node:fs");
const reportsPath = process.env.REPORTS_PATH || 'cypress/results'
const authorURL = process.env.AEM_AUTHOR_URL || 'http://localhost:4502'
const authorName = process.env.AEM_AUTHOR_USERNAME || 'admin'
const authorPass = process.env.AEM_AUTHOR_PASSWORD || 'admin'
const publishURL = process.env.AEM_PUBLISH_URL || 'http://localhost:4503'
const publishName = process.env.AEM_PUBLISH_USERNAME || 'admin'
const publishPass = process.env.AEM_PUBLISH_PASSWORD || 'admin'

let config = {
  // Public values are available synchronously through Cypress.expose().
  expose: {
    AEM_AUTHOR_URL: authorURL,
    AEM_PUBLISH_URL: publishURL,
  },
  // Sensitive values stay in the Node process and are retrieved with cy.env().
  env: {
    AEM_AUTHOR_USERNAME: authorName,
    AEM_AUTHOR_PASSWORD: authorPass,
    AEM_PUBLISH_USERNAME: publishName,
    AEM_PUBLISH_PASSWORD: publishPass,
  },
  e2e: {
    setupNodeEvents(on, config) {
      require('cypress-terminal-report/src/installLogsPrinter')(on, {
        printLogsToConsole: "always",
      });

      // Delete videos for passing tests
      on('after:spec', (spec, results) => {
        if (results && results.video) {
          const failures = results.tests.some((test) =>
              test.attempts.some((attempt) => attempt.state === 'failed')
          );
          if (!failures) {
            unlinkSync(results.video);
          }
        }
      });
    },
    baseUrl: authorURL,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      configFile: 'reporter.config.js',
    },
  },
  video: true,
  videosFolder: reportsPath + "/videos",
  screenshotsFolder: reportsPath + "/screenshots",
}

module.exports = defineConfig(config);
