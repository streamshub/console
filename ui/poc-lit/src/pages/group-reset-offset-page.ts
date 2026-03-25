import { LitElement, html, css } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import type { RouterLocation } from '@vaadin/router';
import { pageStyles } from '../styles/shared-styles';

@customElement('group-reset-offset-page')
export class GroupResetOffsetPage extends LitElement {
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

  render() {
    const groupId = this.location?.params?.groupId as string;

    return html`
      <div class="page-content">
        <div class="placeholder">
          <h2>Reset Consumer Offset</h2>
          <p>Reset offset functionality for consumer group: <strong>${groupId}</strong></p>
          <p>This feature allows you to reset the consumer group's offset to a specific position.</p>
          <p><em>Implementation coming soon...</em></p>
        </div>
      </div>
    `;
  }
}

// Made with Bob
