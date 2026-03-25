import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getTopic } from '../api/topics';
import type { Topic } from '../api/topics';
import { pageStyles } from '../styles/shared-styles';

@customElement('topic-layout')
export class TopicLayout extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  @state() private topicName?: string;

  static styles = [
    pageStyles,
    css`
      :host {
        display: block;
      }

      .topic-tabs {
        border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
        margin-bottom: var(--pf-v5-global--spacer--lg);
      }

      .topic-tabs ul {
        display: flex;
        list-style: none;
        margin: 0;
        padding: 0;
        gap: var(--pf-v5-global--spacer--md);
      }

      .topic-tabs li {
        margin: 0;
      }

      .topic-tabs a {
        display: block;
        padding: var(--pf-v5-global--spacer--md) var(--pf-v5-global--spacer--lg);
        color: var(--pf-v5-global--Color--200);
        text-decoration: none;
        border-bottom: 3px solid transparent;
        transition: all 0.2s;
      }

      .topic-tabs a:hover {
        color: var(--pf-v5-global--Color--100);
        border-bottom-color: var(--pf-v5-global--BorderColor--100);
      }

      .topic-tabs a.active {
        color: var(--pf-v5-global--primary-color--100);
        border-bottom-color: var(--pf-v5-global--primary-color--100);
      }

      .topic-content {
        padding: var(--pf-v5-global--spacer--md);
      }
    `
  ];

  private topicTask = new Task(this, {
    task: async ([kafkaId, topicId]) => {
      const response = await getTopic(
        kafkaId as string,
        topicId as string,
        { fields: ['name'] }
      );
      
      if (response.errors) {
        return null;
      }
      
      return response.data!.data;
    },
    args: () => [
      this.location?.params?.kafkaId,
      this.location?.params?.topicId
    ],
    onComplete: (topic: Topic | null) => {
      if (topic) {
        this.topicName = topic.attributes.name;
      }
    }
  });

  private isActiveRoute(path: string): boolean {
    return this.location?.pathname?.includes(path) || false;
  }

  render() {
    const kafkaId = this.location?.params?.kafkaId as string;
    const topicId = this.location?.params?.topicId as string;

    if (!kafkaId || !topicId) {
      return html`<div>Loading...</div>`;
    }

    const basePath = `/kafka/${kafkaId}/topics/${topicId}`;
    const displayName = this.topicName || topicId;

    return html`
      <div class="topic-layout">
        <div class="page-header">
          <h1>Topic: ${displayName}</h1>
        </div>

        <nav class="topic-tabs">
          <ul>
            <li>
              <a 
                href="${basePath}/messages" 
                class="${this.isActiveRoute('/messages') ? 'active' : ''}"
              >
                Messages
              </a>
            </li>
            <li>
              <a 
                href="${basePath}/partitions" 
                class="${this.isActiveRoute('/partitions') ? 'active' : ''}"
              >
                Partitions
              </a>
            </li>
            <li>
              <a 
                href="${basePath}/groups" 
                class="${this.isActiveRoute('/groups') ? 'active' : ''}"
              >
                Consumer Groups
              </a>
            </li>
            <li>
              <a 
                href="${basePath}/configuration" 
                class="${this.isActiveRoute('/configuration') ? 'active' : ''}"
              >
                Configuration
              </a>
            </li>
          </ul>
        </nav>

        <div class="topic-content">
          <slot></slot>
        </div>
      </div>
    `;
  }
}

// Made with Bob
