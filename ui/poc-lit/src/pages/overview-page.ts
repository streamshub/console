import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { RouterLocation } from '@vaadin/router';

@customElement('overview-page')
export class OverviewPage extends LitElement {
  static styles = css`
    :host {
      display: block;
      padding: var(--pf-v6-global--spacer--lg);
    }

    .page-header {
      margin-bottom: var(--pf-v6-global--spacer--lg);
    }

    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: var(--pf-v6-global--spacer--md);
    }

    .card {
      background: white;
      border: 1px solid var(--pf-v6-global--BorderColor--100);
      border-radius: var(--pf-v6-global--BorderRadius--sm);
      padding: var(--pf-v6-global--spacer--lg);
    }

    .card h3 {
      margin: 0 0 var(--pf-v6-global--spacer--md) 0;
      font-size: var(--pf-v6-global--FontSize--lg);
    }

    .stat {
      font-size: var(--pf-v6-global--FontSize--2xl);
      font-weight: bold;
      color: var(--pf-v6-global--primary-color--100);
    }

    .stat-label {
      font-size: var(--pf-v6-global--FontSize--sm);
      color: var(--pf-v6-global--Color--200);
      margin-top: var(--pf-v6-global--spacer--xs);
    }
  `;

  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v6-c-title pf-m-2xl">Cluster Overview</h1>
        <p style="color: var(--pf-v6-global--Color--200);">
          Overview of Kafka cluster ${this.kafkaId}
        </p>
      </div>

      <div class="cards-grid">
        <div class="card">
          <h3>Cluster Status</h3>
          <div class="stat">✓ Healthy</div>
          <div class="stat-label">All nodes online</div>
        </div>

        <div class="card">
          <h3>Topics</h3>
          <div class="stat">—</div>
          <div class="stat-label">Total topics</div>
        </div>

        <div class="card">
          <h3>Groups</h3>
          <div class="stat">—</div>
          <div class="stat-label">Active groups</div>
        </div>

        <div class="card">
          <h3>Nodes</h3>
          <div class="stat">—</div>
          <div class="stat-label">Broker nodes</div>
        </div>
      </div>

      <div style="margin-top: var(--pf-v6-global--spacer--xl); padding: var(--pf-v6-global--spacer--lg); background: var(--pf-v6-global--info-color--100); color: white; border-radius: var(--pf-v6-global--BorderRadius--sm);">
        <strong>Note:</strong> This is a placeholder page. The full overview page will include:
        <ul>
          <li>Cluster health and status</li>
          <li>Resource metrics (CPU, memory, disk)</li>
          <li>Topic and partition statistics</li>
          <li>Consumer group information</li>
          <li>Recent activity</li>
        </ul>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'overview-page': OverviewPage;
  }
}

// Made with Bob
