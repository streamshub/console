import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { GroupConfigurationTab } from './GroupConfigurationTab';

const configs = {
  'session.timeout.ms': {
    value: '45000',
    source: 'DYNAMIC_GROUP_CONFIG',
    type: 'INT',
    sensitive: false,
    readOnly: false,
  },
  'offsets.retention.minutes': {
    value: '10080',
    source: 'DEFAULT_CONFIG',
    type: 'LONG',
    sensitive: false,
    readOnly: false,
  },
  'group.consumer.assignors': {
    value: 'uniform,range',
    source: 'STATIC_BROKER_CONFIG',
    type: 'STRING',
    sensitive: false,
    readOnly: true,
  },
  'group.coordinator.rebalance.protocols': {
    value: 'classic,consumer',
    source: 'DEFAULT_CONFIG',
    type: 'LIST',
    sensitive: false,
    readOnly: true,
  },
  'group.consumer.heartbeat.interval.ms': {
    value: '5000',
    source: 'DEFAULT_CONFIG',
    type: 'INT',
    sensitive: false,
    readOnly: false,
  },
  'some.secret.value': {
    value: 'hidden',
    source: 'DYNAMIC_GROUP_CONFIG',
    type: 'PASSWORD',
    sensitive: true,
    readOnly: false,
  },
};

const withGroup =
  (groupConfigs: Record<string, unknown>) => (Story: React.ComponentType) =>
    (
      <MemoryRouter>
        <Routes>
          <Route
            element={
              <Outlet
                context={{ group: { attributes: { configs: groupConfigs } } }}
              />
            }
          >
            <Route index element={<Story />} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

const meta = {
  component: GroupConfigurationTab,
  decorators: [withGroup(configs)],
} satisfies Meta<typeof GroupConfigurationTab>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
