import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('kafka-connect-page')
export class KafkaConnectPage extends LitElement {
  static styles = css`
    :host {
      display: block;
      padding: var(--pf-v5-global--spacer--lg);
    }
  `;

  render() {
    return html`
      <h1 class="pf-v5-c-title pf-m-2xl">Kafka Connect</h1>
      <p>Kafka Connect clusters and connectors - to be implemented</p>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'kafka-connect-page': KafkaConnectPage;
  }
}

// Made with Bob
