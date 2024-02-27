import { chromium, Browser, Page } from "playwright";

let browser: Browser;
let page: Page;

const URL = process.env.APP_URL

beforeAll(async () => {
  browser = await chromium.launch();
  page = await browser.newPage();
  await page.goto(
    `${URL}/home`
  );
  await page.click('button[type="submit"]');
  await page.waitForLoadState("networkidle");
  await page.waitForLoadState("networkidle");
}, 100000);

afterAll(async () => {
  await browser.close();
});

export { page };
