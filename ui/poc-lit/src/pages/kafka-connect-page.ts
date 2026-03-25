import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { 
  getConnectors, 
  getConnectClusters, 
  enrichConnectors,
  ConnectorsResponse,
  ConnectClustersResponse 
} from '../api/kafka-connect';
import { pageStyles } from '../styles/shared-styles';
import '../components/connectors-table';
import '../components/connect-clusters-table';

type TabType = 'connectors' | 'connect-clusters';

@customElement('kafka-connect-page')
export class KafkaConnectPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  
  private get kafkaId(): string {
    return this.location?.params.kafkaId as string || '';
  }

  @state() private activeTab: TabType = 'connectors';
  @state() private connectorsPage = 1;
  @state() private connectorsPerPage = 20;
  @state() private connectorsSortBy = 'name';
  @state() private connectorsSortDirection: 'asc' | 'desc' = 'asc';
  @state() private connectorsSearchTerm = '';

  @state() private clustersPage = 1;
  @state() private clustersPerPage = 20;
  @state() private clustersSortBy = 'name';
  @state() private clustersSortDirection: 'asc' | 'desc' = 'asc';
  @state() private clustersSearchTerm = '';

  private connectorsTask = new Task(this, {
    task: async ([kafkaId, page, perPage, sortBy, sortDirection, searchTerm]) => {
      const response = await getConnectors(kafkaId as string, {
        page: page as number,
        perPage: perPage as number,
        sort: sortBy as string,
        sortDir: sortDirection as 'asc' | 'desc',
        name: searchTerm as string || undefined,
      });

      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to load connectors');
      }

      return response.data!;
    },
    args: () => [
      this.kafkaId, 
      this.connectorsPage, 
      this.connectorsPerPage, 
      this.connectorsSortBy, 
      this.connectorsSortDirection, 
      this.connectorsSearchTerm
    ],
  });

  private clustersTask = new Task(this, {
    task: async ([kafkaId, page, perPage, sortBy, sortDirection, searchTerm]) => {
      const response = await getConnectClusters(kafkaId as string, {
        page: page as number,
        perPage: perPage as number,
        sort: sortBy as string,
        sortDir: sortDirection as 'asc' | 'desc',
        name: searchTerm as string || undefined,
      });

      if (response.errors) {
        throw new Error(response.errors[0]?.title || 'Failed to load connect clusters');
      }

      return response.data!;
    },
    args: () => [
      this.kafkaId, 
      this.clustersPage, 
      this.clustersPerPage, 
      this.clustersSortBy, 
      this.clustersSortDirection, 
      this.clustersSearchTerm
    ],
  });

  static styles = [
    pageStyles,
    css`
      .tabs {
        border-bottom: 1px solid var(--pf-v5-global--BorderColor--100);
        margin-bottom: var(--pf-v5-global--spacer--lg);
      }

      .tabs-list {
        display: flex;
        list-style: none;
        padding: 0;
        margin: 0;
        gap: var(--pf-v5-global--spacer--md);
      }

      .tab-button {
        background: none;
        border: none;
        padding: var(--pf-v5-global--spacer--md) var(--pf-v5-global--spacer--lg);
        cursor: pointer;
        color: var(--pf-v5-global--Color--200);
        font-size: var(--pf-v5-global--FontSize--md);
        border-bottom: 3px solid transparent;
        transition: all 0.2s;
      }

      .tab-button:hover {
        color: var(--pf-v5-global--Color--100);
        border-bottom-color: var(--pf-v5-global--BorderColor--100);
      }

      .tab-button.active {
        color: var(--pf-v5-global--primary-color--100);
        border-bottom-color: var(--pf-v5-global--primary-color--100);
        font-weight: var(--pf-v5-global--FontWeight--bold);
      }
    `
  ];

  private handleTabChange(tab: TabType) {
    this.activeTab = tab;
  }

  private handleConnectorsSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.connectorsSearchTerm = input.value;
    this.connectorsPage = 1;
  }

  private handleConnectorsSortChange(e: CustomEvent) {
    this.connectorsSortBy = e.detail.sortBy;
    this.connectorsSortDirection = e.detail.sortDirection;
  }

  private handleConnectorsPreviousPage() {
    if (this.connectorsPage > 1) {
      this.connectorsPage--;
    }
  }

  private handleConnectorsNextPage(hasMore: boolean) {
    if (hasMore) {
      this.connectorsPage++;
    }
  }

  private handleClustersSearch(e: Event) {
    const input = e.target as HTMLInputElement;
    this.clustersSearchTerm = input.value;
    this.clustersPage = 1;
  }

  private handleClustersSortChange(e: CustomEvent) {
    this.clustersSortBy = e.detail.sortBy;
    this.clustersSortDirection = e.detail.sortDirection;
  }

  private handleClustersPreviousPage() {
    if (this.clustersPage > 1) {
      this.clustersPage--;
    }
  }

  private handleClustersNextPage(hasMore: boolean) {
    if (hasMore) {
      this.clustersPage++;
    }
  }

  private renderConnectorsTab() {
    return html`
      <div class="toolbar">
        <div class="search-input">
          <div class="pf-v5-c-input-group">
            <input
              class="pf-v5-c-form-control"
              type="text"
              placeholder="Search connectors..."
              @input=${this.handleConnectorsSearch}
              .value=${this.connectorsSearchTerm}
            />
          </div>
        </div>
      </div>

      ${this.connectorsTask.render({
        pending: () => html`
          <connectors-table loading></connectors-table>
        `,
        complete: (data: ConnectorsResponse) => {
          const enrichedConnectors = enrichConnectors(data.data, data.included);
          return html`
            <connectors-table
              .kafkaId="${this.kafkaId}"
              .connectors=${enrichedConnectors}
              .sortBy=${this.connectorsSortBy}
              .sortDirection=${this.connectorsSortDirection}
              @sort-change=${this.handleConnectorsSortChange}
            ></connectors-table>

            <div class="pagination">
              <div class="pagination-info">
                Showing ${((this.connectorsPage - 1) * this.connectorsPerPage) + 1} - ${Math.min(this.connectorsPage * this.connectorsPerPage, data.meta.page.total || 0)} of ${data.meta.page.total || 0} connectors
              </div>
              <div class="pagination-controls">
                <button
                  class="pf-v5-c-button pf-m-secondary"
                  ?disabled=${this.connectorsPage <= 1}
                  @click=${this.handleConnectorsPreviousPage}
                >
                  Previous
                </button>
                <button
                  class="pf-v5-c-button pf-m-secondary"
                  ?disabled=${this.connectorsPage * this.connectorsPerPage >= (data.meta.page.total || 0)}
                  @click=${() => this.handleConnectorsNextPage(this.connectorsPage * this.connectorsPerPage < (data.meta.page.total || 0))}
                >
                  Next
                </button>
              </div>
            </div>
          `;
        },
        error: (error: unknown) => {
          const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
          return html`
            <div class="error-state">
              <div class="pf-v5-c-empty-state">
                <div class="pf-v5-c-empty-state__content">
                  <div class="pf-v5-c-empty-state__icon">
                    <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
                  </div>
                  <h2 class="pf-v5-c-title pf-m-lg">Error loading connectors</h2>
                  <div class="pf-v5-c-empty-state__body">
                    ${errorMessage}
                  </div>
                  <button
                    class="pf-v5-c-button pf-m-primary"
                    @click=${() => this.connectorsTask.run()}
                  >
                    Retry
                  </button>
                </div>
              </div>
            </div>
          `;
        },
      })}
    `;
  }

  private renderConnectClustersTab() {
    return html`
      <div class="toolbar">
        <div class="search-input">
          <div class="pf-v5-c-input-group">
            <input
              class="pf-v5-c-form-control"
              type="text"
              placeholder="Search connect clusters..."
              @input=${this.handleClustersSearch}
              .value=${this.clustersSearchTerm}
            />
          </div>
        </div>
      </div>

      ${this.clustersTask.render({
        pending: () => html`
          <connect-clusters-table loading></connect-clusters-table>
        `,
        complete: (data: ConnectClustersResponse) => html`
          <connect-clusters-table
            .kafkaId="${this.kafkaId}"
            .clusters=${data.data}
            .sortBy=${this.clustersSortBy}
            .sortDirection=${this.clustersSortDirection}
            @sort-change=${this.handleClustersSortChange}
          ></connect-clusters-table>

          <div class="pagination">
            <div class="pagination-info">
              Showing ${((this.clustersPage - 1) * this.clustersPerPage) + 1} - ${Math.min(this.clustersPage * this.clustersPerPage, data.meta.page.total || 0)} of ${data.meta.page.total || 0} connect clusters
            </div>
            <div class="pagination-controls">
              <button
                class="pf-v5-c-button pf-m-secondary"
                ?disabled=${this.clustersPage <= 1}
                @click=${this.handleClustersPreviousPage}
              >
                Previous
              </button>
              <button
                class="pf-v5-c-button pf-m-secondary"
                ?disabled=${this.clustersPage * this.clustersPerPage >= (data.meta.page.total || 0)}
                @click=${() => this.handleClustersNextPage(this.clustersPage * this.clustersPerPage < (data.meta.page.total || 0))}
              >
                Next
              </button>
            </div>
          </div>
        `,
        error: (error: unknown) => {
          const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
          return html`
            <div class="error-state">
              <div class="pf-v5-c-empty-state">
                <div class="pf-v5-c-empty-state__content">
                  <div class="pf-v5-c-empty-state__icon">
                    <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
                  </div>
                  <h2 class="pf-v5-c-title pf-m-lg">Error loading connect clusters</h2>
                  <div class="pf-v5-c-empty-state__body">
                    ${errorMessage}
                  </div>
                  <button
                    class="pf-v5-c-button pf-m-primary"
                    @click=${() => this.clustersTask.run()}
                  >
                    Retry
                  </button>
                </div>
              </div>
            </div>
          `;
        },
      })}
    `;
  }

  render() {
    return html`
      <div class="page-header">
        <h1 class="pf-v5-c-title pf-m-2xl">Kafka Connect</h1>
      </div>

      <div class="page-content">
        <div class="tabs">
          <ul class="tabs-list" role="tablist">
            <li role="presentation">
              <button
                class="tab-button ${this.activeTab === 'connectors' ? 'active' : ''}"
                role="tab"
                @click=${() => this.handleTabChange('connectors')}
              >
                Connectors
              </button>
            </li>
            <li role="presentation">
              <button
                class="tab-button ${this.activeTab === 'connect-clusters' ? 'active' : ''}"
                role="tab"
                @click=${() => this.handleTabChange('connect-clusters')}
              >
                Connect clusters
              </button>
            </li>
          </ul>
        </div>

        <div class="tab-content">
          ${this.activeTab === 'connectors' 
            ? this.renderConnectorsTab() 
            : this.renderConnectClustersTab()
          }
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'kafka-connect-page': KafkaConnectPage;
  }
}

// Made with Bob
