import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { EnrichedConnector, ConnectorState, ConnectorType } from '../api/kafka-connect';
import { tableStyles, statusStyles } from '../styles/shared-styles';

@customElement('connectors-table')
export class ConnectorsTable extends LitElement {
  @property({ type: Array }) connectors: EnrichedConnector[] = [];
  @property({ type: Boolean }) loading = false;
  @property({ type: String }) sortBy = 'name';
  @property({ type: String }) sortDirection: 'asc' | 'desc' = 'asc';
  @property({ type: String }) kafkaId = '';

  static styles = [
    tableStyles,
    statusStyles,
    css`
      .managed-label {
        display: inline-block;
        margin-left: var(--pf-v5-global--spacer--sm);
        padding: var(--pf-v5-global--spacer--xs) var(--pf-v5-global--spacer--sm);
        background-color: var(--pf-v5-global--BackgroundColor--200);
        border-radius: var(--pf-v5-global--BorderRadius--sm);
        font-size: var(--pf-v5-global--FontSize--xs);
        color: var(--pf-v5-global--Color--200);
      }

      .state-icon {
        display: inline-flex;
        align-items: center;
        gap: var(--pf-v5-global--spacer--xs);
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

  private getStateIcon(state: ConnectorState) {
    switch (state) {
      case 'RUNNING':
        return html`<span class="state-icon status-success">✓ Running</span>`;
      case 'PAUSED':
        return html`<span class="state-icon">⏸ Paused</span>`;
      case 'STOPPED':
        return html`<span class="state-icon">⏹ Stopped</span>`;
      case 'FAILED':
        return html`<span class="state-icon status-danger">✗ Failed</span>`;
      case 'RESTARTING':
        return html`<span class="state-icon">↻ Restarting</span>`;
      case 'UNASSIGNED':
        return html`<span class="state-icon status-warning">⊙ Unassigned</span>`;
      default:
        return html`<span class="state-icon">${state}</span>`;
    }
  }

  private getTypeLabel(type: ConnectorType) {
    switch (type) {
      case 'source':
        return 'Source';
      case 'sink':
        return 'Sink';
      case 'source:mm':
        return 'Mirror Source';
      case 'source:mm-checkpoint':
        return 'Mirror Checkpoint';
      case 'source:mm-heartbeat':
        return 'Mirror Heartbeat';
      default:
        return type;
    }
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
            <div class="pf-v5-c-empty-state__body">Loading connectors...</div>
          </div>
        </div>
      `;
    }

    if (this.connectors.length === 0) {
      return html`
        <div class="pf-v5-c-empty-state">
          <div class="pf-v5-c-empty-state__content">
            <div class="pf-v5-c-empty-state__icon">
              <i class="fas fa-plug" aria-hidden="true"></i>
            </div>
            <h2 class="pf-v5-c-title pf-m-lg">No connectors found</h2>
            <div class="pf-v5-c-empty-state__body">
              There are no connectors in this Kafka cluster yet.
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
            <th role="columnheader">Connect Cluster</th>
            <th role="columnheader">Type</th>
            <th role="columnheader" @click=${() => this.handleSort('state')}>
              State
              <span class="sort-indicator">${this.getSortIndicator('state')}</span>
            </th>
            <th role="columnheader">Tasks</th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          ${this.connectors.map(connector => html`
            <tr role="row">
              <td role="cell" data-label="Name">
                <a href="/kafka/${this.kafkaId}/kafka-connect/${encodeURIComponent(connector.id)}">
                  ${connector.attributes.name}
                </a>
                ${connector.meta?.managed ? html`<span class="managed-label">Managed</span>` : ''}
              </td>
              <td role="cell" data-label="Connect Cluster">
                ${connector.connectClusterId ? html`
                  <a href="/kafka/${this.kafkaId}/kafka-connect/connect-clusters/${connector.connectClusterId}">
                    ${connector.connectClusterName}
                  </a>
                ` : '-'}
              </td>
              <td role="cell" data-label="Type">
                ${this.getTypeLabel(connector.attributes.type)}
              </td>
              <td role="cell" data-label="State">
                ${this.getStateIcon(connector.attributes.state)}
              </td>
              <td role="cell" data-label="Tasks">
                ${connector.replicas ?? '-'}
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
    'connectors-table': ConnectorsTable;
  }
}

// Made with Bob