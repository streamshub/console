import type { Meta, StoryObj } from '@storybook/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { userEvent, within } from 'storybook/test';
import type { KafkaRecord } from '@/api/types';
import { MessageDetails } from './MessageDetails';
import { ThemeProvider } from '@/components/app/ThemeProvider';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

const makeMessage = (overrides: Record<string, unknown> = {}) =>
  ({
    id: 'msg-1',
    attributes: {
      partition: 0,
      offset: 100,
      timestamp: '2026-10-28T10:15:30.123Z',
      key: 'order-1001',
      value: 'plain text value',
      headers: { source: 'checkout', traceId: 'abc-123' },
      size: 512,
    },
    relationships: {},
    meta: {},
    ...overrides,
  } as unknown as KafkaRecord);

// Opens a tab by its visible name
const openTab =
  (name: RegExp) =>
  async ({ canvasElement }: { canvasElement: HTMLElement }) => {
    await userEvent.click(within(canvasElement).getByRole('tab', { name }));
  };

const meta = {
  component: MessageDetails,
  decorators: [
    (Story) => (
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <div>
            <Story />
          </div>
        </QueryClientProvider>
      </ThemeProvider>
    ),
  ],
  args: {
    message: makeMessage(),
  },
} satisfies Meta<typeof MessageDetails>;

export default meta;
type Story = StoryObj<typeof meta>;

export const PlainText: Story = {};

export const JsonValue: Story = {
  args: {
    message: makeMessage({
      attributes: {
        partition: 1,
        offset: 250,
        timestamp: '2026-10-28T10:15:31.456Z',
        key: '{"customerId":42}',
        value:
          '{"orderId":1002,"status":"PAID","items":[{"sku":"A-1","qty":2}]}',
        headers: {},
        size: 2048,
      },
    }),
  },
};

export const NullKeyAndValue: Story = {
  args: {
    message: makeMessage({
      attributes: {
        partition: 0,
        offset: 7,
        timestamp: '2026-10-28T10:15:32.000Z',
        key: null,
        value: null,
        headers: {},
        size: 0,
      },
    }),
  },
};

export const BinaryValue: Story = {
  args: {
    message: makeMessage({
      attributes: {
        partition: 0,
        offset: 300,
        timestamp: '2026-10-28T10:15:33.000Z',
        key: 'binary-event',
        value: 'AQIDBAUGBwgJCg==', // valid base64, so Download works
        headers: {},
        size: 10,
      },
      meta: { content: { value: { type: 'application/octet-stream' } } },
    }),
  },
};

export const BinaryValueOmitted: Story = {
  args: {
    message: makeMessage({
      meta: {
        content: { value: { type: 'application/octet-stream', omitted: true } },
      },
    }),
  },
};

export const WithSchemas: Story = {
  args: {
    message: makeMessage({
      attributes: {
        partition: 2,
        offset: 400,
        timestamp: '2026-10-28T10:15:34.000Z',
        key: 'order-1003',
        value: '{"orderId":1003}',
        headers: {},
        size: 128,
      },
      relationships: {
        keySchema: { meta: { name: 'order-key', artifactType: 'AVRO' } },
        valueSchema: { meta: { name: 'order-value', artifactType: 'JSON' } },
      },
    }),
  },
};

export const KeyTab: Story = {
  play: openTab(/key/i),
};

export const HeadersTab: Story = {
  play: openTab(/headers/i),
};

export const BinaryHeader: Story = {
  args: {
    message: makeMessage({
      attributes: {
        partition: 0,
        offset: 500,
        timestamp: '2026-10-28T10:15:35.000Z',
        key: 'k',
        value: 'v',
        headers: { payload: 'AQID', source: 'checkout' },
        size: 64,
      },
      meta: {
        content: { headers: { payload: { type: 'application/octet-stream' } } },
      },
    }),
  },
  play: openTab(/headers/i),
};
