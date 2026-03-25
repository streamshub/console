import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { KafkaUser } from '../api/kafka-users';
import { formatDateTime } from '../utils/format';
import { tableStyles, utilityStyles } from '../styles/shared-styles';

@customElement('kafka-users-table')
export class KafkaUsersTable extends LitElement {
  static styles = [tableStyles, utilityStyles];

  @property({ type: Array }) users: KafkaUser[] = [];
  @property({ type: String }) sortColumn: string = 'name';
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

  render() {
    return html`
      <table>
        <thead>
          <tr>
            <th class="sortable ${this.sortColumn === 'name' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('name')}>
              Name
            </th>
            <th class="sortable ${this.sortColumn === 'namespace' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('namespace')}>
              Namespace
            </th>
            <th class="sortable ${this.sortColumn === 'creationTimestamp' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('creationTimestamp')}>
              Created
            </th>
            <th class="sortable ${this.sortColumn === 'username' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('username')}>
              Username
            </th>
            <th class="sortable ${this.sortColumn === 'authenticationType' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('authenticationType')}>
              Authentication
            </th>
          </tr>
        </thead>
        <tbody>
          ${this.users.map(user => html`
            <tr>
              <td>
                ${user.meta.privileges?.includes('GET')
                  ? html`<a href="/kafka/${this.kafkaId}/kafka-users/${user.id}">
                      <span class="truncate" title="${user.attributes.name}">${user.attributes.name}</span>
                    </a>`
                  : html`<span class="truncate" title="${user.attributes.name}">${user.attributes.name}</span>`
                }
              </td>
              <td>${user.attributes.namespace || 'n/a'}</td>
              <td>${formatDateTime(user.attributes.creationTimestamp)}</td>
              <td>${user.attributes.username}</td>
              <td>${user.attributes.authenticationType}</td>
            </tr>
          `)}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'kafka-users-table': KafkaUsersTable;
  }
}

// Made with Bob
