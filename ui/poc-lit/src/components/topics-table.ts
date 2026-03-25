import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Topic } from '../api/topics';
import { formatBytes } from '../utils/format';
import { tableStyles, statusStyles } from '../styles/shared-styles';

@customElement('topics-table')
export class TopicsTable extends LitElement {
  @property({ type: Array }) topics: Topic[] = [];
  @property({ type: Boolean }) loading = false;
  @property({ type: String }) sortBy = 'name';
  @property({ type: String }) sortDirection: 'asc' | 'desc' = 'asc';
  @property({ type: String }) kafkaId = '';

  static styles = [tableStyles, statusStyles];

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

  private getStatusIcon(status: Topic['attributes']['status']) {
    switch (status) {
      case 'FullyReplicated':
        return html`<span class="status-icon status-success">✓</span>`;
      case 'UnderReplicated':
      case 'PartiallyOffline':
      case 'Unknown':
        return html`<span class="status-icon status-warning">⚠</span>`;
      case 'Offline':
        return html`<span class="status-icon status-danger">✗</span>`;
      default:
        return html`<span class="status-icon">(${status}) ?</span>`;
    }
  }

  private getSortIndicator(column: string) {
    if (this.sortBy !== column) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
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
            <div class="pf-v6-c-empty-state__body">Loading topics...</div>
          </div>
        </div>
      `;
    }

    if (this.topics.length === 0) {
      return html`
        <div class="pf-v6-c-empty-state">
          <div class="pf-v6-c-empty-state__content">
            <div class="pf-v6-c-empty-state__icon">
              <i class="fas fa-cubes" aria-hidden="true"></i>
            </div>
            <h2 class="pf-v6-c-title pf-m-lg">No topics found</h2>
            <div class="pf-v6-c-empty-state__body">
              There are no topics in this Kafka cluster yet.
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
            <th role="columnheader">Status</th>
            <th role="columnheader" @click=${() => this.handleSort('numPartitions')}>
              Partitions
              <span class="sort-indicator">${this.getSortIndicator('numPartitions')}</span>
            </th>
            <th role="columnheader" @click=${() => this.handleSort('totalLeaderLogBytes')}>
              Storage
              <span class="sort-indicator">${this.getSortIndicator('totalLeaderLogBytes')}</span>
            </th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          ${this.topics.map(topic => html`
            <tr role="row">
              <td role="cell" data-label="Name">
                <a href="/kafka/${this.kafkaId}/topics/${topic.id}">${topic.attributes.name}</a>
              </td>
              <td role="cell" data-label="Status">
                ${this.getStatusIcon(topic.attributes.status)}
                ${topic.attributes.status}
              </td>
              <td role="cell" data-label="Partitions">
                ${topic.attributes.numPartitions}
              </td>
              <td role="cell" data-label="Storage">
                ${formatBytes(topic.attributes.totalLeaderLogBytes)}
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
    'topics-table': TopicsTable;
  }
}

// Made with Bob

