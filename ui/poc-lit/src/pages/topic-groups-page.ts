import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getTopic } from '../api/topics';
import type { Topic } from '../api/topics';
import { pageStyles } from '../styles/shared-styles';

@customElement('topic-groups-page')
export class TopicGroupsPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;

  static styles = [
    pageStyles,
    css`
      :host {
        display: block;
      }

      .placeholder {
        text-align: center;
        padding: var(--pf-v5-global--spacer--2xl);
        color: var(--pf-v5-global--Color--200);
      }

      .placeholder h2 {
        margin-bottom: var(--pf-v5-global--spacer--md);
      }
    `
  ];

  private topicTask = new Task(this, {
    task: async ([kafkaId, topicId]) => {
      const response = await getTopic(
        kafkaId as string,
        topicId as string,
        { fields: ['name', 'groups'] }
      );
      
      if (response.errors) {
        throw new Error(response.errors.map((e: any) => e.detail).join(', '));
      }
      
      return response.data!.data;
    },
    args: () => [
      this.location?.params?.kafkaId,
      this.location?.params?.topicId
    ]
  });

  render() {
    const kafkaId = this.location?.params?.kafkaId as string;

    return html`
      <div class="page-content">
        ${this.topicTask.render({
          pending: () => html`
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Loading consumer groups...</p>
            </div>
          `,
          complete: (topic: Topic) => {
            const groupCount = topic.relationships?.groups?.data?.length || 0;
            return html`
              <div class="placeholder">
                <h2>Consumer Groups</h2>
                <p>This topic is consumed by <strong>${groupCount}</strong> consumer group(s)</p>
                <p><em>Detailed consumer group listing implementation coming soon...</em></p>
              </div>
            `;
          },
          error: (error: unknown) => html`
            <div class="error-state">
              <h3>Error loading consumer groups</h3>
              <p>${error instanceof Error ? error.message : String(error)}</p>
            </div>
          `
        })}
      </div>
    `;
  }
}

// Made with Bob
