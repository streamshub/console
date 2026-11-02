import type { Meta, StoryObj } from '@storybook/react';
import type {
  BrokerStatus,
  ControllerStatus,
  NodeRoles,
  NodeStatuses,
} from '@/api/types';
import {
  useBrokerStatusLabels,
  useBrokerStatusLabelsWithCount,
  useControllerStatusLabels,
  useControllerStatusLabelsWithCount,
  useRoleLabels,
} from './NodeStatusLabel';

const row = {
  display: 'flex',
  gap: 16,
  marginBottom: 8,
  alignItems: 'center',
} as const;
const key = { width: 240, fontFamily: 'monospace' } as const;

function BrokerStatuses() {
  const labels = useBrokerStatusLabels();
  return (
    <div>
      {(Object.keys(labels) as BrokerStatus[]).map((status) => (
        <div key={status} style={row}>
          <span style={key}>{status}</span>
          {labels[status]}
        </div>
      ))}
    </div>
  );
}

function ControllerStatuses() {
  const labels = useControllerStatusLabels();
  return (
    <div>
      {(Object.keys(labels) as ControllerStatus[]).map((status) => (
        <div key={status} style={row}>
          <span style={key}>{status}</span>
          {labels[status]}
        </div>
      ))}
    </div>
  );
}

function Roles({ statuses }: { statuses?: NodeStatuses }) {
  const labels = useRoleLabels(statuses);
  return (
    <div>
      {(Object.keys(labels) as NodeRoles[]).map((role) => (
        <div key={role} style={{ ...row, width: 300 }}>
          <span style={key}>{role}</span>
          <div style={{ flex: 1 }}>{labels[role].labelWithCount}</div>
        </div>
      ))}
    </div>
  );
}

function BrokerStatusesWithCount() {
  const labels = useBrokerStatusLabelsWithCount({
    Running: 3,
    Starting: 1,
    NotRunning: 2,
  });
  return (
    <div style={{ width: 320 }}>
      {(Object.keys(labels) as BrokerStatus[]).map((status) => (
        <div key={status} style={{ marginBottom: 8 }}>
          {labels[status]}
        </div>
      ))}
    </div>
  );
}

function ControllerStatusesWithCount() {
  const labels = useControllerStatusLabelsWithCount({
    QuorumLeader: 1,
    QuorumFollower: 2,
  });
  return (
    <div style={{ width: 320 }}>
      {(Object.keys(labels) as ControllerStatus[]).map((status) => (
        <div key={status} style={{ marginBottom: 8 }}>
          {labels[status]}
        </div>
      ))}
    </div>
  );
}

const meta = {} satisfies Meta;

export default meta;
type Story = StoryObj<typeof meta>;

export const BrokerStatusLabels: Story = { render: () => <BrokerStatuses /> };

export const ControllerStatusLabels: Story = {
  render: () => <ControllerStatuses />,
};

export const RoleLabelsWithCounts: Story = {
  render: () => (
    <Roles
      statuses={
        {
          brokers: { Running: 2, NotRunning: 1 },
          controllers: { QuorumLeader: 1, QuorumFollower: 2 },
        } as unknown as NodeStatuses
      }
    />
  ),
};

export const BrokerFilterOptionsWithCount: Story = {
  render: () => <BrokerStatusesWithCount />,
};

export const ControllerFilterOptionsWithCount: Story = {
  render: () => <ControllerStatusesWithCount />,
};
