// @ts-check
const { defineConfig, devices } = require('@playwright/test');

const reportsPath = process.env.REPORTS_PATH || '/var/task/results'
const proxyServer = process.env.HTTP_PROXY || ''

/**
 * @see https://playwright.dev/docs/test-configuration
 */
let cfg = {
  testDir: './tests',
  outputDir: reportsPath+'/output',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['junit', { outputFile: reportsPath+'/result.xml' }],
    ['html', { outputFolder: reportsPath+'/html', open: 'never' }]
  ],
  use: {
    trace: 'on-first-retry',
    video: {
      mode: 'on',
      size: { width: 1024, height: 768 },
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
}

// enable proxy if set
if (proxyServer !== '') {
  cfg.use.proxy = {
    server: proxyServer,
  }
}

module.exports = defineConfig(cfg);

