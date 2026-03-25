import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import { apiClient } from '../api/client';

interface KafkaCluster {
  id: string;
  attributes: {
    name: string;
    namespace?: string;
    kafkaVersion?: string;
  };
  meta?: {
    authentication?: {
      method: string;
    };
  };
}

interface ClustersResponse {
  data: KafkaCluster[];
  meta: {
    page: {
      total: number;
    };
  };
}

@customElement('home-page')
export class HomePage extends LitElement {
  @state() private searchTerm = '';

  private clustersTask = new Task(this, {
    task: async ([searchTerm]) => {
      const params = new URLSearchParams();
      if (searchTerm) {
        params.set('filter[name][like]', searchTerm as string);
      }
      
      const response = await apiClient.get<ClustersResponse>(
        `/kafkas${params.toString() ? `?${params.toString()}` : ''}`
      );

      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to load clusters');
      }

      return response.data!;
    },
    args: () => [this.searchTerm],
  });

  static styles = css`
    :host {
      display: block;
    }

    .hero {
      background: linear-gradient(to bottom, var(--pf-v5-global--BackgroundColor--200) 0%, var(--pf-v5-global--BackgroundColor--100) 100%);
      padding: var(--pf-v5-global--spacer--xl) var(--pf-v5-global--spacer--lg);
      border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
    }

    .hero h1 {
      font-size: var(--pf-v5-global--FontSize--2xl);
      margin: 0 0 var(--pf-v5-global--spacer--sm) 0;
    }

    .hero p {
      color: var(--pf-v5-global--Color--200);
      margin: 0;
    }

    .content {
      padding: var(--pf-v5-global--spacer--lg);
    }

    .search-bar {
      margin-bottom: var(--pf-v5-global--spacer--md);
    }

    .clusters-table {
      background: white;
      border: 1px solid var(--pf-v5-global--BorderColor--100);
      border-radius: var(--pf-v5-global--BorderRadius--sm);
    }

    .cluster-row {
      padding: var(--pf-v5-global--spacer--md);
      border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .cluster-row:last-child {
      border-bottom: none;
    }

    .cluster-row:hover {
      background-color: var(--pf-v5-global--BackgroundColor--200);
    }

    .cluster-info h3 {
      margin: 0 0 var(--pf-v5-global--spacer--xs) 0;
      font-size: var(--pf-v5-global--FontSize--lg);
    }

    .cluster-meta {
      font-size: var(--pf-v5-global--FontSize--sm);
      color: var(--pf-v5-global--Color--200);
    }

    .cluster-actions a {
      text-decoration: none;
      color: var(--pf-v5-global--link--Color);
    }

    .empty-state {
      text-align: center;
      padding: var(--pf-v5-global--spacer--2xl);
    }
  `;

  private handleSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.searchTerm = input.value;
  }

  private navigateToCluster(clusterId: string) {
    window.location.href = `/kafka/${clusterId}/overview`;
  }

  render() {
    return html`
      <div class="hero">
        <h1>StreamsHub Console</h1>
        <p>Manage your Kafka clusters</p>
      </div>

      <div class="content">
        <div class="search-bar">
          <input
            class="pf-v5-c-form-control"
            type="text"
            placeholder="Search clusters..."
            @input=${this.handleSearch}
            .value=${this.searchTerm}
          />
        </div>

        ${this.clustersTask.render({
          pending: () => html`
            <div class="empty-state">
              <div class="pf-v5-c-spinner pf-m-xl" role="progressbar">
                <span class="pf-v5-c-spinner__clipper"></span>
                <span class="pf-v5-c-spinner__lead-ball"></span>
                <span class="pf-v5-c-spinner__tail-ball"></span>
              </div>
              <p>Loading clusters...</p>
            </div>
          `,
          complete: (data: ClustersResponse) => {
            if (data.data.length === 0) {
              return html`
                <div class="empty-state">
                  <h2>No Kafka clusters found</h2>
                  <p>There are no Kafka clusters configured.</p>
                </div>
              `;
            }

            return html`
              <div class="clusters-table">
                ${data.data.map(cluster => html`
                  <div class="cluster-row" @click=${() => this.navigateToCluster(cluster.id)}>
                    <div class="cluster-info">
                      <h3>${cluster.attributes.name}</h3>
                      <div class="cluster-meta">
                        ${cluster.attributes.namespace ? html`Namespace: ${cluster.attributes.namespace} • ` : ''}
                        ${cluster.attributes.kafkaVersion ? html`Version: ${cluster.attributes.kafkaVersion}` : ''}
                      </div>
                    </div>
                    <div class="cluster-actions">
                      <a href="/kafka/${cluster.id}/overview">View →</a>
                    </div>
                  </div>
                `)}
              </div>
              <p style="margin-top: var(--pf-v5-global--spacer--md); color: var(--pf-v5-global--Color--200);">
                ${data.meta.page.total} cluster${data.meta.page.total !== 1 ? 's' : ''} total
              </p>
            `;
          },
          error: (error: unknown) => {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            return html`
              <div class="empty-state">
                <h2>Error loading clusters</h2>
                <p>${errorMessage}</p>
                <button
                  class="pf-v5-c-button pf-m-primary"
                  @click=${() => this.clustersTask.run()}
                >
                  Retry
                </button>
              </div>
            `;
          },
        })}
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'home-page': HomePage;
  }
}

// Made with Bob
