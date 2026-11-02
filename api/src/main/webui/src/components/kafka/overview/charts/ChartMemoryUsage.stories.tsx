import type { Meta, StoryObj } from '@storybook/react-vite';
import { ChartMemoryUsage } from './ChartMemoryUsage';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

const GiB = 1024 ** 3;

const node0 = {
  '2025-01-15T10:00:00Z': 2 * GiB,
  '2025-01-15T10:05:00Z': 2 * GiB,
  '2025-01-15T10:10:00Z': 2 * GiB,
  '2025-01-15T10:15:00Z': 2 * GiB,
  '2025-01-15T10:20:00Z': 2 * GiB,
};

const node1 = {
  '2025-01-15T10:00:00Z': 3 * GiB,
  '2025-01-15T10:05:00Z': 3 * GiB,
  '2025-01-15T10:10:00Z': 3 * GiB,
  '2025-01-15T10:15:00Z': 3 * GiB,
  '2025-01-15T10:20:00Z': 3 * GiB,
};

const node2 = {
  '2025-01-15T10:00:00Z': 1.5 * GiB,
  '2025-01-15T10:05:00Z': 1.5 * GiB,
  '2025-01-15T10:10:00Z': 1.5 * GiB,
  '2025-01-15T10:15:00Z': 1.5 * GiB,
  '2025-01-15T10:20:00Z': 1.5 * GiB,
};

const meta = {
  component: ChartMemoryUsage,
  args: {
    usages: { '0': node0, '1': node1, '2': node2 },
    duration: DurationOptions.Last24hours,
  },
} satisfies Meta<typeof ChartMemoryUsage>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SingleNode: Story = {
  args: { usages: { '0': node0 } },
};

export const NoData: Story = {
  args: { usages: {} },
};
