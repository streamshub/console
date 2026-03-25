import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getTopic } from '../api/topics';
import type { Topic, Partition } from '../api/topics';
import { pageStyles, tableStyles, statusStyles } from '../styles/shared-styles';
import { formatNumber } from '../utils/format';

@customElement('topic-partitions-page')
export class TopicPartitionsPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;

  static styles = [
    pageStyles,
    tableStyles,
    statusStyles,
    css`
      :host {
        display: block;
      }

      .partition-status {
        display: inline-flex;
        align-items: center;
        gap: var(--pf-v5-global--spacer--xs);
      }

      .replica-list {
        display: flex;
        flex-direction: column;
        gap: var(--pf-v5-global--spacer--xs);
      }

      .replica-item {
        display: flex;
        align-items: center;
        gap: var(--pf-v5-global--spacer--sm);
        font-size: var(--pf-v5-global--FontSize--sm);
      }

      .replica-badge {
        padding: 2px 8px;
        border-radius: var(--pf-v5-global--BorderRadius--sm);
        font-size: var(--pf-v5-global--FontSize--xs);
        background: var(--pf-v5-global--BackgroundColor--200);
      }

      .replica-badge.leader {
        background: var(--pf-v5-global--primary-color--100);
        color: var(--pf-v5-global--Color--light-100);
      }

      .replica-badge.in-sync {
        background: var(--pf-v5-global--success-color--100);
        color: var(--pf-v5-global--Color--light-100);
      }

      .replica-badge.out-of-sync {
        background: var(--pf-v5-global--warning-color--100);
        color: var(--pf-v5-global--Color--dark-100);
      }
    `
  ];

  private topicTask = new Task(this, {
    task: async ([kafkaId, topicId]) => {
      const response = await getTopic(
        kafkaId as string,
        topicId as string,
        { fields: ['name', 'partitions', 'numPartitions'] }
      );
      
      if (response.errors) {
        throw new Error(response.errors.map(e => e.detail).join(', '));
      }
      
      return response.data!.data;
    },
    args: () => [
      this.location?.params?.kafkaId,
      this.location?.params?.topicId
    ]
  });

  private getStatusIcon(status: string) {
    const icons: Record<string, string> = {
      'FullyReplicated': '✓',
      'UnderReplicated': '⚠',
      'Offline': '✕',
    };
    return icons[status] || '?';
  }

  private getStatusClass(status: string) {
    const classes: Record<string, string> = {
      'FullyReplicated': 'status-stable',
      'UnderReplicated': 'status-unknown',
      'Offline': 'status-dead',
    };
    return classes[status] || '';
  }

  private renderPartitionsTable(partitions: Partition[], leaderId?: number) {
    if (!partitions.length) {
      return html`
        <div class="empty-state">
          <p>No partition information available</p>
        </div>
      `;
    }

    return html`
      <div class="table-container">
        <table class="pf-v5-c-table">
          <thead>
            <tr>
              <th>Partition</th>
              <th>Status</th>
              <th>Leader</th>
              <th>Replicas</th>
              <th>Size</th>
              <th>Earliest Offset</th>
              <th>Latest Offset</th>
            </tr>
          </thead>
          <tbody>
            ${partitions.map(partition => html`
              <tr>
                <td>${partition.partition}</td>
                <td>
                  <span class="partition-status">
                    <span class="status-icon ${this.getStatusClass(partition.status)}">
                      ${this.getStatusIcon(partition.status)}
                    </span>
                    ${partition.status}
                  </span>
                </td>
                <td>${partition.leaderId ?? 'N/A'}</td>
                <td>
                  <div class="replica-list">
                    ${partition.replicas.map(replica => html`
                      <div class="replica-item">
                        <span>Node ${replica.nodeId}</span>
                        ${replica.nodeId === partition.leaderId ? html`
                          <span class="replica-badge leader">Leader</span>
                        ` : ''}
                        ${replica.inSync ? html`
                          <span class="replica-badge in-sync">In Sync</span>
                        ` : html`
                          <span class="replica-badge out-of-sync">Out of Sync</span>
                        `}
                        ${replica.localStorage ? html`
                          <span>(${formatNumber(replica.localStorage.size)} bytes)</span>
                        ` : ''}
                      </div>
                    `)}
                  </div>
                </td>
                <td>${formatNumber(partition.leaderLocalStorage)}</td>
                <td>${formatNumber(partition.offsets?.earliest?.offset)}</td>
                <td>${formatNumber(partition.offsets?.latest?.offset)}</td>
              </tr>
            `)}
          </tbody>
        </table>
      </div>
    `;
  }

  render() {
    return html`
      <div class="page-content">
        ${this.topicTask.render({
          pending: () => html`
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Loading partitions...</p>
            </div>
          `,
          complete: (topic: Topic) => {
            if (!topic.attributes.partitions) {
              return html`
                <div class="empty-state">
                  <p>No partition information available for this topic</p>
                </div>
              `;
            }
            return this.renderPartitionsTable(topic.attributes.partitions);
          },
          error: (error: unknown) => html`
            <div class="error-state">
              <h3>Error loading partitions</h3>
              <p>${error instanceof Error ? error.message : String(error)}</p>
            </div>
          `
        })}
      </div>
    `;
  }
}

// Made with Bob
