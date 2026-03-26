import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { RouterLocation } from '@vaadin/router';
import { pageStyles } from '../styles/shared-styles';

@customElement('topic-messages-page')
export class TopicMessagesPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;

  static styles = [
    pageStyles,
    css`
      :host {
        display: block;
      }

      .placeholder {
        text-align: center;
        padding: var(--pf-v6-global--spacer--2xl);
        color: var(--pf-v6-global--Color--200);
      }

      .placeholder h2 {
        margin-bottom: var(--pf-v6-global--spacer--md);
      }
    `
  ];

  render() {
    const topicId = this.location?.params?.topicId as string;

    return html`
      <div class="page-content">
        <div class="placeholder">
          <h2>Topic Messages</h2>
          <p>View and search messages for topic: <strong>${topicId}</strong></p>
          <p><em>Implementation coming soon...</em></p>
        </div>
      </div>
    `;
  }
}

// Made with Bob
