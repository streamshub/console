import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getGroup } from '../api/groups';
import type { Group } from '../api/groups';
import { pageStyles } from '../styles/shared-styles';
import '../components/group-members-table';

@customElement('group-members-page')
export class GroupMembersPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;

  static styles = [
    pageStyles,
    css`
      :host {
        display: block;
      }

      .refresh-info {
        color: var(--pf-v5-global--Color--200);
        font-size: var(--pf-v5-global--FontSize--sm);
        margin-bottom: var(--pf-v5-global--spacer--md);
      }
    `
  ];

  private groupTask = new Task(this, {
    task: async ([kafkaId, groupId]) => {
      const response = await getGroup(
        kafkaId as string,
        groupId as string,
        {
          fields: ['groupId', 'type', 'protocol', 'state', 'simpleConsumerGroup', 'members', 'offsets']
        }
      );
      
      if (response.errors) {
        throw new Error(response.errors.map(e => e.detail).join(', '));
      }
      
      return response.data!.data;
    },
    args: () => [
      this.location?.params?.kafkaId,
      this.location?.params?.groupId
    ]
  });

  connectedCallback() {
    super.connectedCallback();
    // Auto-refresh every 5 seconds
    this.startAutoRefresh();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.stopAutoRefresh();
  }

  private refreshInterval?: number;

  private startAutoRefresh() {
    this.refreshInterval = window.setInterval(() => {
      this.groupTask.run();
    }, 5000);
  }

  private stopAutoRefresh() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = undefined;
    }
  }

  render() {
    const kafkaId = this.location?.params?.kafkaId as string;

    return html`
      <div class="page-content">
        <div class="refresh-info">
          Auto-refreshing every 5 seconds
        </div>

        ${this.groupTask.render({
          pending: () => html`
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Loading group members...</p>
            </div>
          `,
          complete: (group: Group) => html`
            <group-members-table
              .group=${group}
              .kafkaId=${kafkaId}
            ></group-members-table>
          `,
          error: (error: unknown) => html`
            <div class="error-state">
              <h3>Error loading group members</h3>
              <p>${error instanceof Error ? error.message : String(error)}</p>
            </div>
          `
        })}
      </div>
    `;
  }
}

// Made with Bob
