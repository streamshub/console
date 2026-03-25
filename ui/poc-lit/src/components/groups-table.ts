import { LitElement, html } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Group } from '../api/groups';
import { formatNumber } from '../utils/format';
import { tableStyles, badgeStyles, statusStyles } from '../styles/shared-styles';

@customElement('groups-table')
export class GroupsTable extends LitElement {
  static styles = [tableStyles, badgeStyles, statusStyles];

  @property({ type: Array }) groups: Group[] = [];
  @property({ type: String }) sortColumn: string = 'groupId';
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

  private getStateIcon(state: string) {
    const icons: Record<string, string> = {
      'STABLE': '✓',
      'EMPTY': 'ℹ',
      'UNKNOWN': '⚠',
      'DEAD': '✕',
      'PREPARING_REBALANCE': '⏳',
      'COMPLETING_REBALANCE': '↻',
      'ASSIGNING': '⟳',
      'RECONCILING': '⟲',
    };
    return icons[state] || '?';
  }

  private getStateClass(state: string) {
    const classes: Record<string, string> = {
      'STABLE': 'status-stable',
      'EMPTY': 'status-empty',
      'UNKNOWN': 'status-unknown',
      'DEAD': 'status-dead',
    };
    return classes[state] || '';
  }

  private getStateLabel(state: string) {
    return state.split('_').map(word => 
      word.charAt(0) + word.slice(1).toLowerCase()
    ).join(' ');
  }

  private calculateTotalLag(group: Group): number {
    if (!group.attributes.offsets) return 0;
    return group.attributes.offsets.reduce((sum, offset) => {
      return sum + (offset.lag ?? 0);
    }, 0);
  }

  private getUniqueTopics(group: Group): Array<{ name: string; id?: string }> {
    const topicsMap = new Map<string, string | undefined>();
    
    // From member assignments
    group.attributes.members?.forEach(member => {
      member.assignments?.forEach(assignment => {
        topicsMap.set(assignment.topicName, assignment.topicId);
      });
    });
    
    // From offsets
    group.attributes.offsets?.forEach(offset => {
      topicsMap.set(offset.topicName, offset.topicId);
    });
    
    return Array.from(topicsMap.entries()).map(([name, id]) => ({ name, id }));
  }

  render() {
    return html`
      <table>
        <thead>
          <tr>
            <th class="sortable ${this.sortColumn === 'groupId' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('groupId')}>
              Group ID
            </th>
            <th class="sortable ${this.sortColumn === 'type' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('type')}>
              Type
            </th>
            <th class="sortable ${this.sortColumn === 'protocol' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('protocol')}>
              Protocol
            </th>
            <th class="sortable ${this.sortColumn === 'state' ? `sorted-${this.sortDirection}` : ''}"
                @click=${() => this.handleSort('state')}>
              State
            </th>
            <th>Overall Lag</th>
            <th>Members</th>
            <th>Topics</th>
          </tr>
        </thead>
        <tbody>
          ${this.groups.map(group => html`
            <tr>
              <td>
                ${group.meta?.describeAvailable 
                  ? html`<a href="/kafka/${this.kafkaId}/groups/${group.id}">${group.attributes.groupId}</a>`
                  : html`${group.attributes.groupId}`
                }
              </td>
              <td>${group.attributes.type ?? '-'}</td>
              <td>${group.attributes.protocol ?? '-'}</td>
              <td>
                <span class="status-icon ${this.getStateClass(group.attributes.state)}">
                  ${this.getStateIcon(group.attributes.state)}
                </span>
                ${this.getStateLabel(group.attributes.state)}
              </td>
              <td>${formatNumber(this.calculateTotalLag(group))}</td>
              <td>${formatNumber(group.attributes.members?.length ?? 0)}</td>
              <td>
                <div class="label-group">
                  ${this.getUniqueTopics(group).map(topic => html`
                    ${topic.id 
                      ? html`<a href="/kafka/${this.kafkaId}/topics/${topic.id}" class="label label-link">${topic.name}</a>`
                      : html`<span class="label">${topic.name}</span>`
                    }
                  `)}
                </div>
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
    'groups-table': GroupsTable;
  }
}

// Made with Bob
