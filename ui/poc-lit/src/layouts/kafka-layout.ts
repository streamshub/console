import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { apiClient } from '../api/client';

interface ClusterDetail {
  id: string;
  attributes: {
    name: string;
    namespace?: string;
    kafkaVersion?: string;
  };
  meta?: {
    kind?: string;
  };
}

@customElement('kafka-layout')
export class KafkaLayout extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  @state() private sidebarOpen = true;

  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }

  private clusterTask = new Task(this, {
    task: async ([kafkaId]) => {
      if (!kafkaId) {
        throw new Error('No Kafka cluster ID provided');
      }

      const response = await apiClient.get<{ data: ClusterDetail }>(
        `/kafkas/${kafkaId}`
      );

      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to load cluster');
      }

      return response.data!.data;
    },
    args: () => [this.kafkaId],
  });

  static styles = css`
    :host {
      display: block;
      height: 100vh;
    }

    .page-layout {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .masthead {
      background-color: var(--pf-v5-global--BackgroundColor--dark-100);
      color: white;
      padding: var(--pf-v5-global--spacer--md) var(--pf-v5-global--spacer--lg);
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
    }

    .masthead h1 {
      margin: 0;
      font-size: var(--pf-v5-global--FontSize--xl);
      font-weight: normal;
    }

    .masthead a {
      color: white;
      text-decoration: none;
    }

    .page-content {
      display: flex;
      flex: 1;
      overflow: hidden;
    }

    .sidebar {
      width: 250px;
      background-color: var(--pf-v5-global--BackgroundColor--100);
      border-right: 1px solid var(--pf-v5-global--BorderColor--100);
      overflow-y: auto;
      padding: var(--pf-v5-global--spacer--md);
    }

    .sidebar.hidden {
      display: none;
    }

    .nav-group {
      margin-bottom: var(--pf-v5-global--spacer--lg);
    }

    .nav-group-title {
      font-weight: bold;
      margin-bottom: var(--pf-v5-global--spacer--sm);
      font-size: var(--pf-v5-global--FontSize--sm);
      color: var(--pf-v5-global--Color--200);
    }

    .nav-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .nav-item {
      margin-bottom: var(--pf-v5-global--spacer--xs);
    }

    .nav-link {
      display: block;
      padding: var(--pf-v5-global--spacer--sm) var(--pf-v5-global--spacer--md);
      color: var(--pf-v5-global--link--Color);
      text-decoration: none;
      border-radius: var(--pf-v5-global--BorderRadius--sm);
    }

    .nav-link:hover {
      background-color: var(--pf-v5-global--BackgroundColor--200);
    }

    .nav-link.active {
      background-color: var(--pf-v5-global--active-color--100);
      color: white;
    }

    .main-content {
      flex: 1;
      overflow-y: auto;
      background-color: var(--pf-v5-global--BackgroundColor--100);
    }

    .loading-state {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
    }
  `;

  private isActiveRoute(path: string): boolean {
    const context = `/kafka/${this.kafkaId}${path}`;
    return this.location?.pathname?.includes(context) || false;
  }

  private renderNavigation(cluster: ClusterDetail) {
    const isKroxy = cluster.meta?.kind === 'virtualkafkaclusters.kroxylicious.io';
    
    return html`
      <div class="nav-group">
        <div class="nav-group-title">
          Cluster ${cluster.attributes.name}
          ${isKroxy ? html`<span style="color: var(--pf-v5-global--info-color--100);"> (Virtual)</span>` : ''}
        </div>
        <ul class="nav-list">
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/overview" 
              class="nav-link ${this.isActiveRoute('/overview') ? 'active' : ''}"
            >
              Cluster overview
            </a>
          </li>
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/topics" 
              class="nav-link ${this.isActiveRoute('/topics') ? 'active' : ''}"
            >
              Topics
            </a>
          </li>
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/nodes" 
              class="nav-link ${this.isActiveRoute('/nodes') ? 'active' : ''}"
            >
              Kafka nodes
            </a>
          </li>
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/kafka-connect" 
              class="nav-link ${this.isActiveRoute('/kafka-connect') ? 'active' : ''}"
            >
              Kafka Connect
            </a>
          </li>
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/kafka-users" 
              class="nav-link ${this.isActiveRoute('/kafka-users') ? 'active' : ''}"
            >
              Kafka users
            </a>
          </li>
          <li class="nav-item">
            <a 
              href="/kafka/${this.kafkaId}/groups" 
              class="nav-link ${this.isActiveRoute('/groups') ? 'active' : ''}"
            >
              Groups
            </a>
          </li>
        </ul>
      </div>
    `;
  }

  render() {
    return html`
      <div class="page-layout">
        <header class="masthead">
          <h1><a href="/">StreamsHub Console</a></h1>
        </header>

        <div class="page-content">
          ${this.clusterTask.render({
            pending: () => html`
              <div class="loading-state">
                <div class="pf-v5-c-spinner pf-m-xl" role="progressbar">
                  <span class="pf-v5-c-spinner__clipper"></span>
                  <span class="pf-v5-c-spinner__lead-ball"></span>
                  <span class="pf-v5-c-spinner__tail-ball"></span>
                </div>
              </div>
            `,
            complete: (cluster: ClusterDetail) => html`
              <aside class="sidebar ${this.sidebarOpen ? '' : 'hidden'}">
                ${this.renderNavigation(cluster)}
              </aside>
              <main class="main-content">
                <slot></slot>
              </main>
            `,
            error: (error: unknown) => {
              const errorMessage = error instanceof Error ? error.message : 'Unknown error';
              return html`
                <div class="loading-state">
                  <div style="text-align: center;">
                    <h2>Error loading cluster</h2>
                    <p>${errorMessage}</p>
                    <button
                      class="pf-v5-c-button pf-m-primary"
                      @click=${() => this.clusterTask.run()}
                    >
                      Retry
                    </button>
                  </div>
                </div>
              `;
            },
          })}
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'kafka-layout': KafkaLayout;
  }
}

// Made with Bob
