import { expect, type Page } from '@playwright/test';

export class AuthenticatedPage {
  constructor(public readonly page: Page) {}

  async goToClusterOverview() {
    await this.page.goto('./', { waitUntil: 'domcontentloaded' });

    const view = this.page.getByRole('button', { name: 'View' }).first();

    await expect(view).toBeVisible({ timeout: 15000 });

    await view.click();

    // Wait for the destination URL without requiring the full load event.
    await this.page.waitForURL('**/overview', {
      waitUntil: 'domcontentloaded',
      timeout: 15000,
    });

    await expect(
      this.page.getByRole('heading', { name: 'Cluster overview' })
    ).toBeVisible({ timeout: 15000 });
  }

  async clickNav(name: string) {
    await this.page
      .locator('#page-sidebar')
      .getByRole('link', { name })
      .click();
  }

  async clickLink(text: string, where: 'main' | 'sidebar' = 'main') {
    await this.page
      .locator(where === 'main' ? 'main' : '#page-sidebar')
      .locator('a')
      .locator(`text="${text}"`)
      .click();
  }

  async clickTab(text: string, where: 'main' | 'sidebar' = 'main') {
    const tabsContainer = this.page
      .locator(where === 'main' ? 'main' : '#page-sidebar')
      .locator('[role="tablist"]');
    const tab = tabsContainer.getByRole('tab', { name: text });
    await expect(tab).toBeVisible();
    await tab.click({ force: true });
    await expect(tab).toHaveAttribute('aria-selected', 'true', {
      timeout: 10000,
    });
  }

  async goToTopics(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav('Topics');
    await expect(this.page.locator('h1').getByText('Topics')).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToFirstTopic(waitForLoaded = true) {
    await this.goToTopics(waitForLoaded);
    await this.clickFirstLinkInTheTable('topics-table');

    if (waitForLoaded) {
      await expect(
        this.page.getByLabel('Breadcrumb').getByRole('link', { name: 'Topics' })
      ).toBeVisible();

      // Wait until the topic detail page has finished loading.
      await expect(
        this.page.getByRole('heading', { name: 'Loading topic...' })
      ).toBeHidden({ timeout: 15000 });

      await expect(
        this.page.getByRole('tab', { name: 'Configuration' })
      ).toBeVisible({ timeout: 15000 });
    }
  }

  async goToGroups(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav('Groups');
    await expect(this.page.locator('h1').getByText('Groups')).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async goToKafkaUser(waitForLoaded = true) {
    await this.goToClusterOverview();
    await this.clickNav('Kafka Users');
    await expect(
      this.page.locator('h1').getByText('Kafka Users')
    ).toBeVisible();
    if (waitForLoaded) {
      await this.waitForTableLoaded();
    }
  }

  async waitForTableLoaded() {
    await expect(
      this.page
        .locator('div')
        .filter({ hasText: /^Loading data$/ })
        .first()
    ).toBeHidden();
  }

  async clickFirstLinkInTheTable(ouiaId: string, dataLabel?: string) {
    await this.page
      .locator(`table[data-ouia-component-id="${ouiaId}"]`)
      .locator('tbody')
      .locator('tr')
      .first()
      .locator(`td${dataLabel ? `[data-label="${dataLabel}"]` : ''} a`)
      .first()
      .click();
  }
}
