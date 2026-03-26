import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('not-found-page')
export class NotFoundPage extends LitElement {
  static styles = css`
    :host {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      text-align: center;
    }

    .content {
      max-width: 500px;
    }

    h1 {
      font-size: 4rem;
      margin: 0;
      color: var(--pf-v6-global--danger-color--100);
    }

    h2 {
      margin: var(--pf-v6-global--spacer--md) 0;
    }
  `;

  render() {
    return html`
      <div class="content">
        <h1>404</h1>
        <h2>Page Not Found</h2>
        <p>The page you're looking for doesn't exist.</p>
        <a href="/" class="pf-v6-c-button pf-m-primary">
          Go to Home
        </a>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'not-found-page': NotFoundPage;
  }
}

// Made with Bob
