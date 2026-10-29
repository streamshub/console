import type { Meta, StoryObj } from '@storybook/react-vite';
import { ClusterChartsCardView } from './ClusterChartsCard';
import { DurationOptions } from './utils/types';

const GiB = 1024 ** 3;

const times = [
  '2025-01-15T10:00:00Z',
  '2025-01-15T10:05:00Z',
  '2025-01-15T10:10:00Z',
  '2025-01-15T10:15:00Z',
  '2025-01-15T10:20:00Z',
];

const flatSeries = (value: number): Record<string, number> =>
  Object.fromEntries(times.map((t) => [t, value]));

// Used disk space per node (bytes)
const diskUsages = {
  '0': flatSeries(20 * GiB),
  '1': flatSeries(25 * GiB),
  '2': flatSeries(18 * GiB),
};

// Disk capacity / available storage threshold per node (bytes)
const diskAvailable = {
  '0': flatSeries(50 * GiB),
  '1': flatSeries(50 * GiB),
  '2': flatSeries(50 * GiB),
};

// CPU usage per node (cores, 0.25 = 250m)
const cpuUsages = {
  '0': flatSeries(0.25),
  '1': flatSeries(0.3),
  '2': flatSeries(0.2),
};

// Memory usage per node (bytes)
const memoryUsages = {
  '0': flatSeries(2 * GiB),
  '1': flatSeries(3 * GiB),
  '2': flatSeries(1.5 * GiB),
};

const meta = {
  component: ClusterChartsCardView,
  parameters: { layout: 'padded' },
  decorators: [
    (Story) => (
      <div style={{ maxWidth: 900 }}>
        <Story />
      </div>
    ),
  ],
  args: {
    nodes: [
      { id: 0, name: 'broker-0' },
      { id: 1, name: 'broker-1' },
      { id: 2, name: 'broker-2' },
    ],
    metricsAvailable: true,
    selectedBroker: null,
    onBrokerChange: () => {},
    selectedDuration: DurationOptions.Last5minutes,
    onDurationChange: () => {},
    isLoadingMetrics: false,
    hasError: false,
    diskUsageData: { usages: diskUsages, available: diskAvailable },
    cpuUsageData: cpuUsages,
    memoryUsageData: memoryUsages,
  },
} satisfies Meta<typeof ClusterChartsCardView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Loading: Story = {
  args: { isLoadingMetrics: true },
};

export const ErrorLoadingMetrics: Story = {
  args: { hasError: true },
};

export const MetricsUnavailable: Story = {
  args: { metricsAvailable: false },
};
