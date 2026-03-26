import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { tableStyles } from '../styles/shared-styles';
import type { Group, MemberDescription, OffsetAndMetadata } from '../api/groups';

@customElement('group-members-table')
export class GroupMembersTable extends LitElement {
  @property({ type: Object }) group?: Group;
  @property({ type: String }) kafkaId = '';
  @state() private expandedRows = new Set<string>();

  static styles = [
    tableStyles,
    css`
      :host {
        display: block;
      }

      .lag-table {
        margin: var(--pf-v6-global--spacer--md) 0;
        background: var(--pf-v6-global--BackgroundColor--200);
        padding: var(--pf-v6-global--spacer--md);
      }

      .lag-table table {
        width: 100%;
      }

      .empty-state {
        text-align: center;
        padding: var(--pf-v6-global--spacer--xl);
        color: var(--pf-v6-global--Color--200);
      }

      .help-icon {
        color: var(--pf-v6-global--Color--200);
        margin-left: var(--pf-v6-global--spacer--xs);
        cursor: help;
      }

      .expandable-row {
        cursor: pointer;
      }

      .expandable-row:hover {
        background: var(--pf-v6-global--BackgroundColor--200);
      }

      .expand-icon {
        transition: transform 0.2s;
        display: inline-block;
        margin-right: var(--pf-v6-global--spacer--sm);
      }

      .expand-icon.expanded {
        transform: rotate(90deg);
      }
    `
  ];

  private toggleRow(memberId: string) {
    if (this.expandedRows.has(memberId)) {
      this.expandedRows.delete(memberId);
    } else {
      this.expandedRows.add(memberId);
    }
    this.requestUpdate();
  }

  private calculateOverallLag(member: MemberDescription): number {
    if (!this.group?.attributes.offsets || !member.assignments) {
      return 0;
    }

    const partitions = member.assignments.map(a => ({
      topicName: a.topicName,
      partition: a.partition
    }));

    return this.group.attributes.offsets
      .filter(o => partitions.some(p => p.topicName === o.topicName && p.partition === o.partition))
      .reduce((acc, o) => acc + (o.lag ?? 0), 0);
  }

  private getMemberOffsets(member: MemberDescription): OffsetAndMetadata[] {
    if (!this.group?.attributes.offsets || !member.assignments) {
      return [];
    }

    const offsets = member.assignments.map(a => {
      const offset = this.group!.attributes.offsets?.find(
        o => o.topicId === a.topicId && o.partition === a.partition
      );
      return {
        ...a,
        ...offset,
        topicName: a.topicName,
        partition: a.partition,
        offset: offset?.offset ?? null,
        logEndOffset: offset?.logEndOffset,
        lag: offset?.lag
      } as OffsetAndMetadata;
    });

    return offsets.sort((a, b) => a.topicName.localeCompare(b.topicName));
  }

  private renderLagTable(offsets: OffsetAndMetadata[]) {
    if (!offsets.length) {
      return html`
        <div class="empty-state">
          <p>No offset information available</p>
        </div>
      `;
    }

    return html`
      <div class="lag-table">
        <table class="pf-v6-c-table pf-m-compact">
          <thead>
            <tr>
              <th>Topic</th>
              <th>Partition</th>
              <th>Lag</th>
              <th>
                Committed Offset
                <span class="help-icon" title="The last offset that was committed by the consumer">ⓘ</span>
              </th>
              <th>
                End Offset
                <span class="help-icon" title="The last offset in the partition">ⓘ</span>
              </th>
            </tr>
          </thead>
          <tbody>
            ${offsets.map(offset => html`
              <tr>
                <td>
                  <a href="/kafka/${this.kafkaId}/topics/${offset.topicId || offset.topicName}">
                    ${offset.topicName}
                  </a>
                </td>
                <td>${offset.partition}</td>
                <td>${offset.lag ?? 'N/A'}</td>
                <td>${offset.offset ?? 'N/A'}</td>
                <td>${offset.logEndOffset ?? 'N/A'}</td>
              </tr>
            `)}
          </tbody>
        </table>
      </div>
    `;
  }

  render() {
    if (!this.group) {
      return html`
        <div class="empty-state">
          <p>Loading group information...</p>
        </div>
      `;
    }

    let members: MemberDescription[] = [];

    // Handle simple consumer groups (no members but has offsets)
    if (this.group.attributes.members?.length === 0 && this.group.attributes.offsets) {
      members = [{
        memberId: 'unknown',
        clientId: 'unknown',
        host: 'N/A',
        assignments: this.group.attributes.offsets.map(o => ({
          topicId: o.topicId,
          topicName: o.topicName,
          partition: o.partition
        }))
      }];
    } else {
      members = this.group.attributes.members || [];
    }

    if (!members.length) {
      return html`
        <div class="empty-state">
          <p>No members found for this consumer group</p>
        </div>
      `;
    }

    return html`
      <div class="table-container">
        <table class="pf-v6-c-table pf-m-compact">
          <thead>
            <tr>
              <th width="30%">Member ID</th>
              <th width="20%">
                Client ID
                <span class="help-icon" title="The client ID of the consumer">ⓘ</span>
              </th>
              <th>
                Overall Lag
                <span class="help-icon" title="Total lag across all assigned partitions">ⓘ</span>
              </th>
              <th>Assigned Partitions</th>
            </tr>
          </thead>
          <tbody>
            ${members.map(member => {
              const isExpanded = this.expandedRows.has(member.memberId);
              const offsets = this.getMemberOffsets(member);
              
              return html`
                <tr class="expandable-row" @click=${() => this.toggleRow(member.memberId)}>
                  <td>
                    <span class="expand-icon ${isExpanded ? 'expanded' : ''}">▶</span>
                    ${member.memberId}
                  </td>
                  <td>${member.clientId}</td>
                  <td>${this.calculateOverallLag(member)}</td>
                  <td>${member.assignments?.length ?? 0}</td>
                </tr>
                ${isExpanded ? html`
                  <tr>
                    <td colspan="4">
                      ${this.renderLagTable(offsets)}
                    </td>
                  </tr>
                ` : ''}
              `;
            })}
          </tbody>
        </table>
      </div>
    `;
  }
}

// Made with Bob
