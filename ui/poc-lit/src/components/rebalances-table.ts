import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Rebalance } from '../api/rebalances';
import { formatDateTime, formatRelativeTime } from '../utils/format';
import { tableStyles, statusStyles, badgeStyles } from '../styles/shared-styles';

@customElement('rebalances-table')
export class RebalancesTable extends LitElement {
  @property({ type: Array }) rebalances: Rebalance[] = [];
  @property({ type: Boolean }) loading = false;
  @property({ type: String }) sortColumn = 'name';
  @property({ type: String }) sortDirection: 'asc' | 'desc' = 'asc';
  @property({ type: String }) kafkaId = '';

  static styles = [tableStyles, statusStyles, badgeStyles];

  private handleSort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }

    this.dispatchEvent(new CustomEvent('sort-change', {
      detail: { column: this.sortColumn, direction: this.sortDirection },
      bubbles: true,
      composed: true,
    }));
  }

  private getSortIndicator(column: string) {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private getStatusClass(status: string | null): string {
    if (!status) return 'status-unknown';
    
    switch (status) {
      case 'Ready':
      case 'ProposalReady':
        return 'status-success';
      case 'Rebalancing':
      case 'PendingProposal':
        return 'status-info';
      case 'Stopped':
      case 'ReconciliationPaused':
        return 'status-warning';
      case 'NotReady':
        return 'status-danger';
      default:
        return 'status-unknown';
    }
  }

  private getModeLabel(mode: string): string {
    switch (mode) {
      case 'full':
        return 'Full';
      case 'add-brokers':
        return 'Add Brokers';
      case 'remove-brokers':
        return 'Remove Brokers';
      default:
        return mode;
    }
  }

  render() {
    if (this.loading) {
      return html`
        <div class="pf-v6-c-empty-state">
          <div class="pf-v6-c-empty-state__content">
            <div class="pf-v6-c-spinner pf-m-xl" role="progressbar">
              <span class="pf-v6-c-spinner__clipper"></span>
              <span class="pf-v6-c-spinner__lead-ball"></span>
              <span class="pf-v6-c-spinner__tail-ball"></span>
            </div>
            <div class="pf-v6-c-empty-state__body">Loading rebalances...</div>
          </div>
        </div>
      `;
    }

    if (this.rebalances.length === 0) {
      return html`
        <div class="pf-v6-c-empty-state">
          <div class="pf-v6-c-empty-state__content">
            <div class="pf-v6-c-empty-state__icon">
              <i class="fas fa-balance-scale" aria-hidden="true"></i>
            </div>
            <h2 class="pf-v6-c-title pf-m-lg">No rebalances found</h2>
            <div class="pf-v6-c-empty-state__body">
              There are no rebalances for this Kafka cluster.
            </div>
          </div>
        </div>
      `;
    }

    return html`
      <table class="pf-v6-c-table pf-m-grid-md" role="grid">
        <thead>
          <tr role="row">
            <th role="columnheader" @click=${() => this.handleSort('name')}>
              Name
              <span class="sort-indicator">${this.getSortIndicator('name')}</span>
            </th>
            <th role="columnheader" @click=${() => this.handleSort('status')}>
              Status
              <span class="sort-indicator">${this.getSortIndicator('status')}</span>
            </th>
            <th role="columnheader">Mode</th>
            <th role="columnheader">Brokers</th>
            <th role="columnheader" @click=${() => this.handleSort('lastUpdated')}>
              Last Updated
              <span class="sort-indicator">${this.getSortIndicator('lastUpdated')}</span>
            </th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          ${this.rebalances.map(rebalance => html`
            <tr role="row">
              <td role="cell" data-label="Name">
                <a href="/kafka/${this.kafkaId}/nodes/rebalances/${rebalance.id}">
                  ${rebalance.attributes.name}
                </a>
              </td>
              <td role="cell" data-label="Status">
                <span class="status-icon ${this.getStatusClass(rebalance.attributes.status)}">
                  ${rebalance.attributes.status || 'Unknown'}
                </span>
              </td>
              <td role="cell" data-label="Mode">
                <span class="badge badge-info">
                  ${this.getModeLabel(rebalance.attributes.mode)}
                </span>
              </td>
              <td role="cell" data-label="Brokers">
                ${rebalance.attributes.brokers?.length || 0} broker${rebalance.attributes.brokers?.length === 1 ? '' : 's'}
              </td>
              <td role="cell" data-label="Last Updated">
                <span title="${formatDateTime(rebalance.attributes.creationTimestamp)}">
                  ${formatRelativeTime(rebalance.attributes.creationTimestamp)}
                </span>
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
    'rebalances-table': RebalancesTable;
  }
}

// Made with Bob