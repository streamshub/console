import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import { apiClient } from '../api/client';
import '../components/pf/pf-text-input';
import '../components/pf/pf-button';
import '../components/pf/pf-card';
import '../components/pf/pf-empty-state';

interface KafkaCluster {
  id: string;
  attributes: {
    name: string;
    namespace?: string;
    kafkaVersion?: string;
    status?: string;
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
        params.set('filter[name]', `like,*${searchTerm}*` as string);
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
      background: linear-gradient(to bottom, var(--pf-v6-global--BackgroundColor--200) 0%, var(--pf-v6-global--BackgroundColor--100) 100%);
      padding: var(--pf-v6-global--spacer--xl) var(--pf-v6-global--spacer--lg);
      border-bottom: 1px solid var(--pf-v6-global--BorderColor--100);
    }

    .hero h1 {
      font-size: var(--pf-v6-global--FontSize--2xl);
      margin: 0 0 var(--pf-v6-global--spacer--sm) 0;
    }

    .hero p {
      color: var(--pf-v6-global--Color--200);
      margin: 0;
    }

    .content {
      padding: var(--pf-v6-global--spacer--lg);
    }

    .search-bar {
      margin-bottom: var(--pf-v6-global--spacer--md);
    }

    .table-container {
      background: white;
      border: 1px solid var(--pf-v6-global--BorderColor--100);
      border-radius: var(--pf-v6-global--BorderRadius--sm);
      overflow: hidden;
    }

    .cluster-name {
      font-weight: var(--pf-v6-global--FontWeight--bold);
    }

    .cluster-name a {
      color: var(--pf-v6-global--link--Color);
      text-decoration: none;
    }

    .cluster-name a:hover {
      text-decoration: underline;
    }

    .cluster-meta {
      font-size: var(--pf-v6-global--FontSize--sm);
      color: var(--pf-v6-global--Color--200);
    }

    .empty-state {
      text-align: center;
      padding: var(--pf-v6-global--spacer--2xl);
    }

    .table-footer {
      padding: var(--pf-v6-global--spacer--md);
      color: var(--pf-v6-global--Color--200);
      font-size: var(--pf-v6-global--FontSize--sm);
      border-top: 1px solid var(--pf-v6-global--BorderColor--100);
    }

    tr {
      cursor: pointer;
    }

    tr:hover {
      background-color: var(--pf-v6-global--BackgroundColor--200);
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
          <pf-text-input
            type="search"
            placeholder="Search clusters..."
            .value=${this.searchTerm}
            @input=${this.handleSearch}
          ></pf-text-input>
        </div>

        ${this.clustersTask.render({
          pending: () => html`
            <div class="empty-state">
              <div class="pf-v6-c-spinner pf-m-xl" role="progressbar">
                <span class="pf-v6-c-spinner__clipper"></span>
                <span class="pf-v6-c-spinner__lead-ball"></span>
                <span class="pf-v6-c-spinner__tail-ball"></span>
              </div>
              <p>Loading clusters...</p>
            </div>
          `,
          complete: (data: ClustersResponse) => {
            if (data.data.length === 0) {
              return html`
                <pf-empty-state title="No Kafka clusters found">
                  <p>There are no Kafka clusters configured.</p>
                </pf-empty-state>
              `;
            }

            return html`
              <div class="table-container">
                <table class="pf-v6-c-table pf-m-compact" role="grid">
                  <caption>Kafka Clusters</caption>
                  <thead class="pf-v6-c-table__thead">
                    <tr class="pf-v6-c-table__tr" role="row">
                      <th class="pf-v6-c-table__th" role="columnheader" scope="col">
                        Name
                      </th>
                      <th class="pf-v6-c-table__th" role="columnheader" scope="col">
                        Kafka Version
                      </th>
                      <th class="pf-v6-c-table__th" role="columnheader" scope="col">
                        Status
                      </th>
                      <th class="pf-v6-c-table__th" role="columnheader" scope="col">
                        Namespace
                      </th>
                    </tr>
                  </thead>
                  <tbody class="pf-v6-c-table__tbody" role="rowgroup">
                    ${data.data.map(cluster => html`
                      <tr
                        class="pf-v6-c-table__tr"
                        role="row"
                        @click=${() => this.navigateToCluster(cluster.id)}
                      >
                        <td class="pf-v6-c-table__td" role="cell">
                          <div class="cluster-name">
                            <a href="/kafka/${cluster.id}/overview" @click=${(e: Event) => e.stopPropagation()}>
                              ${cluster.attributes.name}
                            </a>
                          </div>
                        </td>
                        <td class="pf-v6-c-table__td" role="cell">
                          ${cluster.attributes.kafkaVersion || 'N/A'}
                        </td>
                        <td class="pf-v6-c-table__td" role="cell">
                          ${cluster.attributes.status || 'Unknown'}
                        </td>
                        <td class="pf-v6-c-table__td" role="cell">
                          ${cluster.attributes.namespace || 'N/A'}
                        </td>
                      </tr>
                    `)}
                  </tbody>
                </table>
                <div class="table-footer">
                  ${data.meta.page.total} cluster${data.meta.page.total !== 1 ? 's' : ''} total
                </div>
              </div>
            `;
          },
          error: (error: unknown) => {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            return html`
              <pf-empty-state title="Error loading clusters">
                <p>${errorMessage}</p>
                <pf-button
                  slot="actions"
                  variant="primary"
                  @click=${() => this.clustersTask.run()}
                >
                  Retry
                </pf-button>
              </pf-empty-state>
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
