import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { ConnectCluster } from '../api/kafka-connect';
import { tableStyles } from '../styles/shared-styles';

@customElement('connect-clusters-table')
export class ConnectClustersTable extends LitElement {
  @property({ type: Array }) clusters: ConnectCluster[] = [];
  @property({ type: Boolean }) loading = false;
  @property({ type: String }) sortBy = 'name';
  @property({ type: String }) sortDirection: 'asc' | 'desc' = 'asc';
  @property({ type: String }) kafkaId = '';

  static styles = [
    tableStyles,
    css`
      .help-icon {
        margin-left: var(--pf-v5-global--spacer--xs);
        color: var(--pf-v5-global--Color--200);
        cursor: help;
      }
    `
  ];

  private handleSort(column: string) {
    if (this.sortBy === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDirection = 'asc';
    }

    this.dispatchEvent(new CustomEvent('sort-change', {
      detail: { sortBy: this.sortBy, sortDirection: this.sortDirection },
      bubbles: true,
      composed: true,
    }));
  }

  private getSortIndicator(column: string) {
    if (this.sortBy !== column) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  render() {
    if (this.loading) {
      return html`
        <div class="pf-v5-c-empty-state">
          <div class="pf-v5-c-empty-state__content">
            <div class="pf-v5-c-spinner pf-m-xl" role="progressbar">
              <span class="pf-v5-c-spinner__clipper"></span>
              <span class="pf-v5-c-spinner__lead-ball"></span>
              <span class="pf-v5-c-spinner__tail-ball"></span>
            </div>
            <div class="pf-v5-c-empty-state__body">Loading connect clusters...</div>
          </div>
        </div>
      `;
    }

    if (this.clusters.length === 0) {
      return html`
        <div class="pf-v5-c-empty-state">
          <div class="pf-v5-c-empty-state__content">
            <div class="pf-v5-c-empty-state__icon">
              <i class="fas fa-server" aria-hidden="true"></i>
            </div>
            <h2 class="pf-v5-c-title pf-m-lg">No connect clusters found</h2>
            <div class="pf-v5-c-empty-state__body">
              There are no Kafka Connect clusters in this Kafka cluster yet.
            </div>
          </div>
        </div>
      `;
    }

    return html`
      <table class="pf-v5-c-table pf-m-grid-md" role="grid">
        <thead>
          <tr role="row">
            <th role="columnheader" @click=${() => this.handleSort('name')}>
              Name
              <span class="sort-indicator">${this.getSortIndicator('name')}</span>
            </th>
            <th role="columnheader" @click=${() => this.handleSort('version')}>
              Version
              <span class="sort-indicator">${this.getSortIndicator('version')}</span>
            </th>
            <th role="columnheader" title="Number of worker replicas in the connect cluster">
              Workers
              <span class="help-icon">ⓘ</span>
            </th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          ${this.clusters.map(cluster => html`
            <tr role="row">
              <td role="cell" data-label="Name">
                <a href="/kafka/${this.kafkaId}/kafka-connect/connect-clusters/${cluster.id}">
                  ${cluster.attributes.name}
                </a>
              </td>
              <td role="cell" data-label="Version">
                ${cluster.attributes.version || '-'}
              </td>
              <td role="cell" data-label="Workers">
                ${cluster.attributes.replicas ?? '-'}
              </td>
            </tr>
          `)}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'connect-clusters-table': ConnectClustersTable;
  }
}

// Made with Bob