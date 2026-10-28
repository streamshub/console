import { test as base } from '@playwright/test';
import { AuthenticatedPage } from './authenticated-page';

// Declare the types of your fixtures.
type MyFixtures = {
  authenticatedPage: AuthenticatedPage;
};

// Extend base test by providing "todoPage" and "settingsPage".
// This new "test" can be used in multiple test files, and each of them will get the fixtures.
export const test = base.extend<MyFixtures>({
  authenticatedPage: async ({ page }, use) => {
    // Set up the fixture.
    const todoPage = new AuthenticatedPage(page);

    // Playwright fixture callback, not a React Hook.
    // eslint-disable-next-line react-hooks/rules-of-hooks
    await use(todoPage);
  },
});
export { expect } from '@playwright/test';
