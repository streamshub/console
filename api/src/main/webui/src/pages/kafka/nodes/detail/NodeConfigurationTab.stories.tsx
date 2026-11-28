import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter, Route, Routes } from 'react-router';
import { expect, userEvent, within } from 'storybook/test';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NodeConfigurationTab } from './NodeConfigurationTab';

const KAFKA_ID = 'k1-id';
const NODE_ID = '0';

const configs = {
  'log.retention.hours': {
    type: 'INT',
    value: '168',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'num.io.threads': {
    type: 'INT',
    value: '8',
    source: 'DYNAMIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  listeners: {
    type: 'STRING',
    value: 'PLAINTEXT://:9092,CONTROLLER://:9093',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: true,
  },
  'ssl.keystore.password': {
    type: 'PASSWORD',
    value: 'hidden',
    source: 'DYNAMIC_BROKER_CONFIG',
    sensitive: true,
    readOnly: false,
  },
};

const sortableConfigs = {
  'd.static': {
    type: 'STRING',
    value: '3',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'b.default': {
    type: 'STRING',
    value: '4',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'c.dynamic': {
    type: 'STRING',
    value: '1',
    source: 'DYNAMIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'a.default': {
    type: 'STRING',
    value: '2',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
};

function propertyNames(canvasElement: HTMLElement): string[] {
  return Array.from(canvasElement.querySelectorAll('tbody tr'))
    .map((row) => row.querySelector('[data-label="Property"]'))
    .filter((cell): cell is Element => Boolean(cell))
    .map((cell) => cell.firstElementChild!.textContent!.trim());
}

const originalFetch = window.fetch;

function withNode(nodeConfigs: object, status = 200) {
  return (Story: React.ComponentType) => {
    const body =
      status === 200
        ? {
            data: { id: NODE_ID, type: 'nodeConfigs', attributes: nodeConfigs },
          }
        : { errors: [{ status: String(status), detail: 'Unable to fetch' }] };

    window.fetch = async (input, init) => {
      const url =
        typeof input === 'string'
          ? input
          : input instanceof URL
          ? input.href
          : input.url;

      if (url.includes('/api/')) {
        return new Response(JSON.stringify(body), {
          status,
          headers: { 'Content-Type': 'application/json' },
        });
      }
      return originalFetch(input, init);
    };

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[`/kafka/${KAFKA_ID}/nodes/${NODE_ID}/configuration`]}
        >
          <Routes>
            <Route
              path="/kafka/:kafkaId/nodes/:nodeId/configuration"
              element={<Story />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };
}

const meta = {
  component: NodeConfigurationTab,
  decorators: [
    (Story, context) =>
      withNode(
        (context.parameters.nodeConfigs as object | undefined) ?? configs,
        (context.parameters.apiStatus as number | undefined) ?? 200
      )(Story),
  ],
} satisfies Meta<typeof NodeConfigurationTab>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const NoConfiguration: Story = {
  parameters: { nodeConfigs: {} },
};

export const ErrorState: Story = {
  parameters: { apiStatus: 500 },
};

export const SortBySourceUsesSourceValues: Story = {
  parameters: { nodeConfigs: sortableConfigs },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const sourceHeader = await canvas.findByRole('columnheader', {
      name: /source/i,
    });
    const propertyHeader = canvas.getByRole('columnheader', {
      name: /property/i,
    });

    await userEvent.click(within(sourceHeader).getByRole('button'));
    expect(sourceHeader).toHaveAttribute('aria-sort', 'ascending');
    expect(propertyHeader).not.toHaveAttribute('aria-sort', 'ascending');
    expect(propertyNames(canvasElement)).toEqual([
      'a.default',
      'b.default',
      'c.dynamic',
      'd.static',
    ]);

    await userEvent.click(within(sourceHeader).getByRole('button'));
    expect(sourceHeader).toHaveAttribute('aria-sort', 'descending');
    expect(propertyNames(canvasElement)).toEqual([
      'd.static',
      'c.dynamic',
      'a.default',
      'b.default',
    ]);
  },
};
