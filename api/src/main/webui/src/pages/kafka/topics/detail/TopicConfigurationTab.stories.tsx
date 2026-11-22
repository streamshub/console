import type { Meta, StoryObj } from '@storybook/react-vite'; // or '@storybook/react'
import { MemoryRouter, Route, Routes } from 'react-router';
import { expect, userEvent, within } from 'storybook/test';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TopicConfigurationTab } from './TopicConfigurationTab';

const KAFKA_ID = 'k1-id';
const TOPIC_ID = 'topic-1';

const configs = {
  'cleanup.policy': {
    type: 'LIST',
    value: 'delete',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'retention.ms': {
    type: 'LONG',
    value: '604800000',
    source: 'DYNAMIC_TOPIC_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'max.message.bytes': {
    type: 'INT',
    value: '1048588',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'segment.bytes': {
    type: 'INT',
    value: '1073741824',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: true,
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
    source: 'DYNAMIC_TOPIC_CONFIG',
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
  return Array.from(canvasElement.querySelectorAll('tbody tr')).map((row) =>
    row.querySelector('td')!.firstElementChild!.textContent!.trim()
  );
}

const originalFetch = window.fetch;

function withTopic(topicConfigs: object, privileges: string[] = ['UPDATE']) {
  return (Story: React.ComponentType) => {
    const body = {
      data: {
        id: TOPIC_ID,
        type: 'topics',
        attributes: { name: 'orders', configs: topicConfigs },
        meta: { privileges }, // adjust to whatever hasPrivilege() reads
      },
    };

    window.fetch = async (input, init) => {
      const url =
        typeof input === 'string'
          ? input
          : input instanceof URL
          ? input.href
          : input.url;

      if (url.includes('/api/')) {
        return new Response(JSON.stringify(body), {
          status: 200,
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
          initialEntries={[
            `/kafka/${KAFKA_ID}/topics/${TOPIC_ID}/configuration`,
          ]}
        >
          <Routes>
            <Route
              path="/kafka/:kafkaId/topics/:topicId/configuration"
              element={<Story />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };
}

const meta = {
  component: TopicConfigurationTab,
} satisfies Meta<typeof TopicConfigurationTab>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  decorators: [withTopic(configs)],
};

export const ReadOnlyUser: Story = {
  decorators: [withTopic(configs, [])],
};

export const NoConfiguration: Story = {
  decorators: [withTopic({})],
};

export const SortBySourceUsesSourceValues: Story = {
  decorators: [withTopic(sortableConfigs)],
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
      'b.default',
      'a.default',
    ]);
  },
};
