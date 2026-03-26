import { LitElement, html, css } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import { Task } from '@lit/task';
import type { RouterLocation } from '@vaadin/router';
import { getTopic } from '../api/topics';
import type { Topic, ConfigValue } from '../api/topics';
import { pageStyles, tableStyles } from '../styles/shared-styles';

@customElement('topic-configuration-page')
export class TopicConfigurationPage extends LitElement {
  @property({ type: Object }) location?: RouterLocation;
  @state() private propertyFilter = '';
  @state() private selectedDataSources: string[] = [];

  static styles = [
    pageStyles,
    tableStyles,
    css`
      :host {
        display: block;
      }

      .filters {
        display: flex;
        gap: var(--pf-v6-global--spacer--md);
        margin-bottom: var(--pf-v6-global--spacer--md);
        align-items: center;
      }

      .filter-input {
        flex: 1;
        max-width: 400px;
      }

      .filter-input input {
        width: 100%;
        padding: var(--pf-v6-global--spacer--sm);
        border: 1px solid var(--pf-v6-global--BorderColor--100);
        border-radius: var(--pf-v6-global--BorderRadius--sm);
      }

      .filter-chips {
        display: flex;
        gap: var(--pf-v6-global--spacer--sm);
        flex-wrap: wrap;
        margin-bottom: var(--pf-v6-global--spacer--md);
      }

      .chip {
        display: inline-flex;
        align-items: center;
        gap: var(--pf-v6-global--spacer--xs);
        padding: var(--pf-v6-global--spacer--xs) var(--pf-v6-global--spacer--sm);
        background: var(--pf-v6-global--BackgroundColor--200);
        border: 1px solid var(--pf-v6-global--BorderColor--100);
        border-radius: var(--pf-v6-global--BorderRadius--sm);
        font-size: var(--pf-v6-global--FontSize--sm);
      }

      .chip button {
        background: none;
        border: none;
        cursor: pointer;
        padding: 0;
        color: var(--pf-v6-global--Color--200);
      }

      .chip button:hover {
        color: var(--pf-v6-global--danger-color--100);
      }

      .data-source-filter {
        display: flex;
        gap: var(--pf-v6-global--spacer--sm);
        flex-wrap: wrap;
      }

      .data-source-filter label {
        display: flex;
        align-items: center;
        gap: var(--pf-v6-global--spacer--xs);
        cursor: pointer;
      }

      .reset-button {
        padding: var(--pf-v6-global--spacer--sm) var(--pf-v6-global--spacer--md);
        background: var(--pf-v6-global--BackgroundColor--100);
        border: 1px solid var(--pf-v6-global--BorderColor--100);
        border-radius: var(--pf-v6-global--BorderRadius--sm);
        cursor: pointer;
      }

      .reset-button:hover {
        background: var(--pf-v6-global--BackgroundColor--200);
      }

      .config-value {
        font-family: monospace;
      }

      .sensitive-value {
        color: var(--pf-v6-global--Color--200);
        font-style: italic;
      }
    `
  ];

  private topicTask = new Task(this, {
    task: async ([kafkaId, topicId]) => {
      const response = await getTopic(
        kafkaId as string,
        topicId as string,
        { fields: ['name', 'configs'] }
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

  private handlePropertyFilterChange(e: Event) {
    this.propertyFilter = (e.target as HTMLInputElement).value;
  }

  private handleDataSourceToggle(source: string) {
    if (this.selectedDataSources.includes(source)) {
      this.selectedDataSources = this.selectedDataSources.filter(s => s !== source);
    } else {
      this.selectedDataSources = [...this.selectedDataSources, source];
    }
  }

  private handleReset() {
    this.propertyFilter = '';
    this.selectedDataSources = [];
  }

  private renderConfigTable(config: Record<string, ConfigValue>) {
    const allEntries = Object.entries(config);
    
    // Get unique data sources
    const dataSources = Array.from(new Set(allEntries.map(([_, value]) => value.source)));

    // Filter entries
    const filteredEntries = allEntries
      .filter(([key]) => !this.propertyFilter || key.toLowerCase().includes(this.propertyFilter.toLowerCase()))
      .filter(([_, value]) => 
        this.selectedDataSources.length === 0 || this.selectedDataSources.includes(value.source)
      )
      .sort((a, b) => a[0].localeCompare(b[0]));

    return html`
      <div class="filters">
        <div class="filter-input">
          <input
            type="text"
            placeholder="Filter by property name..."
            .value=${this.propertyFilter}
            @input=${this.handlePropertyFilterChange}
          />
        </div>
        <button class="reset-button" @click=${this.handleReset}>
          Reset Filters
        </button>
      </div>

      ${this.selectedDataSources.length > 0 ? html`
        <div class="filter-chips">
          ${this.selectedDataSources.map(source => html`
            <span class="chip">
              Data source: ${source}
              <button @click=${() => this.handleDataSourceToggle(source)}>✕</button>
            </span>
          `)}
        </div>
      ` : ''}

      <div class="data-source-filter">
        <strong>Filter by data source:</strong>
        ${dataSources.map(source => html`
          <label>
            <input
              type="checkbox"
              .checked=${this.selectedDataSources.includes(source)}
              @change=${() => this.handleDataSourceToggle(source)}
            />
            ${source}
          </label>
        `)}
      </div>

      ${filteredEntries.length === 0 ? html`
        <div class="empty-state">
          <p>No configuration properties match the current filters</p>
        </div>
      ` : html`
        <div class="table-container">
          <table class="pf-v6-c-table">
            <thead>
              <tr>
                <th width="30%">Property</th>
                <th width="40%">Value</th>
                <th>Source</th>
                <th>Type</th>
              </tr>
            </thead>
            <tbody>
              ${filteredEntries.map(([key, value]) => html`
                <tr>
                  <td>${key}</td>
                  <td>
                    ${value.sensitive ? html`
                      <span class="sensitive-value">(sensitive)</span>
                    ` : html`
                      <span class="config-value">${value.value}</span>
                    `}
                  </td>
                  <td>${value.source}</td>
                  <td>${value.type}</td>
                </tr>
              `)}
            </tbody>
          </table>
        </div>
      `}
    `;
  }

  render() {
    return html`
      <div class="page-content">
        ${this.topicTask.render({
          pending: () => html`
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Loading configuration...</p>
            </div>
          `,
          complete: (topic: Topic) => {
            if (!topic.attributes.configs) {
              return html`
                <div class="empty-state">
                  <p>No configuration available for this topic</p>
                </div>
              `;
            }
            return this.renderConfigTable(topic.attributes.configs);
          },
          error: (error: unknown) => html`
            <div class="error-state">
              <h3>Error loading configuration</h3>
              <p>${error instanceof Error ? error.message : String(error)}</p>
            </div>
          `
        })}
      </div>
    `;
  }
}

// Made with Bob
