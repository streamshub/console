import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { GroupMembersTab } from './GroupMembersTab';

// Static mock data
const members = [
  {
    memberId: 'consumer-1-abc123',
    host: '/10.0.0.1',
    clientId: 'orders-service',
    assignments: [
      { topicId: 'topic-1', topicName: 'orders', partition: 0 },
      { topicId: 'topic-1', topicName: 'orders', partition: 1 },
    ],
  },
  {
    memberId: 'consumer-2-def456',
    host: '/10.0.0.2',
    clientId: 'payments-service',
    assignments: [{ topicId: 'topic-2', topicName: 'payments', partition: 0 }],
  },
];

const offsets = [
  {
    topicId: 'topic-1',
    topicName: 'orders',
    partition: 0,
    offset: 1500,
    lag: 12,
  },
  {
    topicId: 'topic-1',
    topicName: 'orders',
    partition: 1,
    offset: 1480,
    lag: 30,
  },
  {
    topicId: 'topic-2',
    topicName: 'payments',
    partition: 0,
    offset: 900,
    lag: 0,
  },
];

const withGroup =
  (attributes: Record<string, unknown>) => (Story: React.ComponentType) =>
    (
      <MemoryRouter>
        <Routes>
          <Route element={<Outlet context={{ group: { attributes } }} />}>
            <Route index element={<Story />} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

const meta = {
  component: GroupMembersTab,
  decorators: [withGroup({ members, offsets })],
} satisfies Meta<typeof GroupMembersTab>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
