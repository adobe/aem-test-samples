// @ts-check
const { test, expect } = require('@playwright/test');

const authorURL = process.env.AEM_AUTHOR_URL || 'http://localhost'
const authorName = process.env.AEM_AUTHOR_USERNAME || 'username'
const authorPass = process.env.AEM_AUTHOR_PASSWORD || 'password'
const publishURL = process.env.AEM_PUBLISH_URL || 'http://localhost'
const publishName = process.env.AEM_PUBLISH_USERNAME || 'username'
const publishPass = process.env.AEM_PUBLISH_PASSWORD || 'password'

test('has title', async ({ page }) => {
  await page.goto(authorURL);
  await expect(page).toHaveTitle("AEM Sign In");
});

test('login to author', async ({ page }) => {
  await page.goto(authorURL);

  await page.locator('#coral-id-0').click()
  await expect(page.locator('#login')).toHaveAttribute("action", "/libs/granite/core/content/login.html/j_security_check")

  await page.locator('#username').fill(authorName)
  await page.locator('#password').fill(authorPass)
  await page.locator('#submit-button').click()

  await expect(page).toHaveTitle("AEM Start");
  await expect(page.getByRole('heading')).toHaveText("Navigation");
});
