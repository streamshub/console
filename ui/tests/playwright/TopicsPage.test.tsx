import { expect, test } from "@playwright/test";

test("Topics page", async ({page}) => {
  await test.step("Navigate to topics page", async () => {
    await page.goto("./");
    await page.click('text="Click to login anonymously"');
    await page.click('text="Topics"');
    await page.waitForSelector('text="Hide internal topics"', { timeout: 500000 });
  })
  await test.step("Topics page should display table", async () => {
    const label =  page.locator('label.pf-v5-c-switch').first();
    expect(label).not.toBeNull();
    const labelText = await label?.innerText();
    expect(labelText?.trim()).toBe("Hide internal topics");
    const input =   label?.locator('input.pf-v5-c-switch__input').first();
    expect(input).not.toBeNull();
    const isChecked = await input?.isChecked();
    expect(isChecked).toBe(true);
    //const button = await page.$('button:has-text("Create Topic")');
    //expect(button).not.toBeNull();
    const filterInput =  page.locator('div.pf-v5-c-input-group input').first();
    expect(filterInput).not.toBeNull();

    // Get the placeholder attribute value
    const placeholderValue = await filterInput?.getAttribute("placeholder");
    expect(placeholderValue).toBe("Filter by name");
    expect(await page.innerText("body")).toContain("Name");
    expect(await page.innerText("body")).toContain("Status");
    expect(await page.innerText("body")).toContain("Partitions");
    expect(await page.innerText("body")).toContain("Consumer groups");
    expect(await page.innerText("body")).toContain("Storage");
  });
});
