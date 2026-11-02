import type { Meta, StoryObj } from '@storybook/react';
import { fn } from 'storybook/test'; // or '@storybook/test', matching your other stories
import type { KafkaRecord } from '@/api/types';
import type { Column } from './ColumnsModal';
import { MessagesTable } from './MessagesTable';

const allColumns = [
  'offset-partition',
  'timestampUTC',
  'timestamp',
  'key',
  'headers',
  'value',
  'size',
] as Column[];

const defaultColumns = [
  'offset-partition',
  'timestampUTC',
  'key',
  'headers',
  'value',
  'size',
] as Column[];

const messages = [
  {
    id: 'msg-1',
    attributes: {
      partition: 0,
      offset: 100,
      timestamp: '2026-10-28T10:15:30.123Z',
      key: 'order-1001',
      value: '{"orderId":1001,"status":"CREATED"}',
      headers: { source: 'checkout', traceId: 'abc-123' },
      size: 512,
    },
    relationships: {},
    meta: {},
  },
  {
    id: 'msg-2',
    attributes: {
      partition: 1,
      offset: 101,
      timestamp: '2026-10-28T10:15:31.456Z',
      key: null,
      value:
        '{"orderId":1002,"status":"PAID","items":[{"sku":"A-1","qty":2},{"sku":"B-7","qty":1}]}',
      headers: {},
      size: 2048,
    },
    relationships: {},
    meta: {},
  },
  {
    id: 'msg-3',
    attributes: {
      partition: 1,
      offset: 102,
      timestamp: '2026-10-28T10:15:32.789Z',
      key: 'binary-key',
      value: '',
      headers: { payload: 'AQID' },
      size: 3 * 1024 * 1024,
    },
    relationships: {
      valueSchema: {
        meta: {
          name: 'order-value-schema',
          errors: [{ detail: 'Schema validation failed' }],
        },
      },
    },
    meta: {
      content: {
        key: { type: 'application/octet-stream' },
        headers: { payload: { type: 'application/octet-stream' } },
      },
    },
  },
] as unknown as KafkaRecord[];

const meta = {
  component: MessagesTable,
  args: {
    messages,
    chosenColumns: defaultColumns,
    hasFilters: false,
    topicName: 'orders',
    onSelectMessage: fn(),
    onReset: fn(),
  },
} satisfies Meta<typeof MessagesTable>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const AllColumns: Story = {
  args: { chosenColumns: allColumns },
};

export const WithSelectedMessage: Story = {
  args: { selectedMessage: messages[1] },
};

export const Loading: Story = {
  args: { isLoading: true },
};

export const NoMessages: Story = {
  args: { messages: [] },
};

export const NoResultsWithFilters: Story = {
  args: { messages: [], hasFilters: true },
};

export const ErrorState: Story = {
  args: {
    errorState: <div style={{ padding: 16 }}>Failed to load messages</div>,
  },
};
