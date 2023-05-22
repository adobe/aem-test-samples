const { defineConfig } = require("cypress");

const reportsPath = process.env.REPORTS_PATH || 'cypress/results'
const authorURL = process.env.AEM_AUTHOR_URL || 'http://localhost:4502'
const authorName = process.env.AEM_AUTHOR_USERNAME || 'admin'
const authorPass = process.env.AEM_AUTHOR_PASSWORD || 'admin'
const publishURL = process.env.AEM_PUBLISH_URL || 'http://localhost:4503'
const publishName = process.env.AEM_PUBLISH_USERNAME || 'admin'
const publishPass = process.env.AEM_PUBLISH_PASSWORD || 'admin'

let config = {
  env: {
    AEM_AUTHOR_URL: authorURL,
    AEM_AUTHOR_USERNAME: authorName,
    AEM_AUTHOR_PASSWORD: authorPass,
    AEM_PUBLISH_URL: publishURL,
    AEM_PUBLISH_USERNAME: publishName,
    AEM_PUBLISH_PASSWORD: publishPass,
    REPORTS_PATH: reportsPath,
  },
  e2e: {
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
    baseUrl: authorURL,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      configFile: 'reporter.config.js',
    },
  },
  videosFolder: reportsPath + "/videos",
  screenshotsFolder: reportsPath + "/screenshots",
}

module.exports = defineConfig(config);
