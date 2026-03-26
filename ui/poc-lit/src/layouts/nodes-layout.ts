import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { RouterLocation } from '@vaadin/router';

@customElement('nodes-layout')
export class NodesLayout extends LitElement {
  @property({ type: Object }) location?: RouterLocation;

  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }

  private get currentPath(): string {
    return this.location?.pathname || '';
  }

  static styles = css`
    :host {
      display: block;
    }

    .tabs {
      border-bottom: 1px solid var(--pf-v6-global--BorderColor--100);
      background-color: var(--pf-v6-global--BackgroundColor--100);
    }

    .tabs-list {
      display: flex;
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .tab-item {
      margin: 0;
    }

    .tab-link {
      display: block;
      padding: var(--pf-v6-global--spacer--md) var(--pf-v6-global--spacer--lg);
      text-decoration: none;
      color: var(--pf-v6-global--Color--100);
      border-bottom: 3px solid transparent;
      transition: border-color 0.2s;
    }

    .tab-link:hover {
      border-bottom-color: var(--pf-v6-global--BorderColor--200);
    }

    .tab-link.active {
      border-bottom-color: var(--pf-v6-global--primary-color--100);
      color: var(--pf-v6-global--primary-color--100);
      font-weight: var(--pf-v6-global--FontWeight--bold);
    }

    .tab-content {
      display: block;
    }
  `;

  private isActiveTab(path: string): boolean {
    return this.currentPath === path || this.currentPath.startsWith(path + '/');
  }

  render() {
    const overviewPath = `/kafka/${this.kafkaId}/nodes`;
    const rebalancesPath = `/kafka/${this.kafkaId}/nodes/rebalances`;

    return html`
      <div class="tabs">
        <ul class="tabs-list" role="tablist">
          <li class="tab-item" role="presentation">
            <a
              href="${overviewPath}"
              class="tab-link ${this.isActiveTab(overviewPath) && !this.currentPath.includes('/rebalances') ? 'active' : ''}"
              role="tab"
            >
              Overview
            </a>
          </li>
          <li class="tab-item" role="presentation">
            <a
              href="${rebalancesPath}"
              class="tab-link ${this.isActiveTab(rebalancesPath) ? 'active' : ''}"
              role="tab"
            >
              Rebalance
            </a>
          </li>
        </ul>
      </div>
      <div class="tab-content">
        <slot></slot>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'nodes-layout': NodesLayout;
  }
}

// Made with Bob