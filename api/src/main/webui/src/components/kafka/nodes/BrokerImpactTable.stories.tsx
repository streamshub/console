import type { Meta, StoryObj } from '@storybook/react';
import type { BrokerCapacity, BrokerLoadImpact } from '@/api/types';
import { BrokerImpactTable } from './BrokerImpactTable';

type Impact = Record<string, Record<string, BrokerLoadImpact>>;

// Builds one metric entry; diff is derived so the numbers stay consistent
const m = (before: number, after: number) =>
  ({ before, after, diff: after - before } as unknown as BrokerLoadImpact);

// Metric keys must match the ones used in COLUMN_GROUPS in the component
const brokerMetrics = (seed: number) => ({
  diskUsedPercentage: m(40 + seed, 45 + seed),
  diskUsedMB: m(4000 + seed * 100, 4500 + seed * 100),
  cpuPercentage: m(20 + seed, 20 + seed), // no change, shows "-" in the delta column
  leaders: m(10, 12 - seed),
  replicas: m(30, 30),
  leaderNetworkInRateKB: m(500 + seed * 10, 650 + seed * 10),
  networkOutRateKB: m(800, 700),
});

const brokerImpact: Impact = {
  '0': brokerMetrics(0),
  '1': brokerMetrics(1),
  '2': brokerMetrics(2),
};

// 25 brokers so pagination (20 per page) has a second page
const manyBrokers: Impact = Object.fromEntries(
  Array.from({ length: 25 }, (_, i) => [String(i), brokerMetrics(i % 10)])
);

const brokerCapacity = {
  cpu: '1',
  inboundNetwork: '10000KB/s',
  outboundNetwork: '10000KB/s',
  overrides: [
    {
      brokers: [2],
      cpu: '2',
      inboundNetwork: '20000KB/s',
      outboundNetwork: '20000KB/s',
    },
  ],
} as unknown as BrokerCapacity;

const meta = {
  component: BrokerImpactTable,
  args: {
    brokerImpact,
    brokerCapacity,
  },
} satisfies Meta<typeof BrokerImpactTable>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithoutCapacity: Story = {
  args: { brokerCapacity: undefined },
};

export const ManyBrokers: Story = {
  args: { brokerImpact: manyBrokers },
};

export const NoData: Story = {
  args: { brokerImpact: null },
};
