import { chromium, Browser, Page } from "playwright";
import  { beforeAll, afterAll }from '@jest/globals';

let browser: Browser;
let page: Page;

const username = process.env.APP_USERNAME ?? "";
const password = process.env.APP_PASSWORD ?? "";

beforeAll(async () => {
  browser = await chromium.launch();
  page = await browser.newPage();
  await page.goto(
    "https://console.amq-streams-ui.us-east.containers.appdomain.cloud/home"
  );
  await page.click('button[type="submit"]');
  await page.waitForLoadState("networkidle");
  await page.fill("#username", username);
  await page.fill("#password", password);
  await page.click('input[type="submit"]');
  await page.waitForLoadState("networkidle");
}, 100000);

afterAll(async () => {
  await browser.close();
});

export { page };
