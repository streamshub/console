import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Node } from '../api/nodes';
import { formatNumber, formatBytes } from '../utils/format';
import { tableStyles, badgeStyles, statusStyles } from '../styles/shared-styles';

@customElement('nodes-table')
export class NodesTable extends LitElement {
  static styles = [tableStyles, badgeStyles, statusStyles];

  @property({ type: Array }) nodes: Node[] = [];
  @property({ type: String }) sortColumn: string = 'id';
  @property({ type: String }) sortDirection: 'asc' | 'desc' = 'asc';
  @property({ type: String }) kafkaId: string = '';

  private handleSort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.dispatchEvent(new CustomEvent('sort-change', {
      detail: { column: this.sortColumn, direction: this.sortDirection }
    }));
  }

  private getStatusClass(status: string): string {
    if (status === 'Running' || status === 'QuorumLeader' || status === 'QuorumFollower') {
      return 'status-running';
    }
    if (status === 'QuorumFollowerLagged' || status === 'Starting' || status === 'Recovery') {
      return 'status-warning';
    }
    return 'status-error';
  }

  private getStatusIcon(status: string): string {
    if (status === 'Running' || status === 'QuorumLeader' || status === 'QuorumFollower') {
      return '✓';
    }
    if (status === 'QuorumFollowerLagged' || status === 'Starting' || status === 'Recovery') {
      return '⚠';
    }
    return '✕';
  }

  render() {
    return html`
      <table>
        <thead>
          <tr>
            <th class="sortable ${this.sortColumn === 'id' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('id')}>
              Node ID
            </th>
            <th>Roles</th>
            <th>Status</th>
            <th>Replicas</th>
            <th>Rack</th>
            <th>Node Pool</th>
          </tr>
        </thead>
        <tbody>
          ${this.nodes.map(node => html`
            <tr>
              <td>
                <a href="/kafka/${this.kafkaId}/nodes/${node.id}">${node.id}</a>
              </td>
              <td>
                ${node.attributes.roles?.map(role => html`
                  <span class="badge badge-${role}">${role}</span>
                `)}
              </td>
              <td>
                ${node.attributes.broker ? html`
                  <span class="${this.getStatusClass(node.attributes.broker.status)}">
                    ${this.getStatusIcon(node.attributes.broker.status)}
                    ${node.attributes.broker.status}
                  </span>
                ` : ''}
                ${node.attributes.controller ? html`
                  <span class="${this.getStatusClass(node.attributes.controller.status)}">
                    ${this.getStatusIcon(node.attributes.controller.status)}
                    ${node.attributes.controller.status}
                  </span>
                ` : ''}
              </td>
              <td>
                ${node.attributes.broker ? html`
                  Leaders: ${formatNumber(node.attributes.broker.leaderCount)}<br/>
                  Followers: ${formatNumber(node.attributes.broker.replicaCount)}
                ` : '-'}
              </td>
              <td>${node.attributes.rack || '-'}</td>
              <td>${node.attributes.nodePool || '-'}</td>
            </tr>
          `)}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'nodes-table': NodesTable;
  }
}

// Made with Bob
