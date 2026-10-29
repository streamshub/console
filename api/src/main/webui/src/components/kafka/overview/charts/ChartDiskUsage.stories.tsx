import type { Meta, StoryObj } from '@storybook/react-vite';
import { ChartDiskUsage } from './ChartDiskUsage';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

const GiB = 1024 ** 3;

function makeSeries(value: number): Record<string, number> {
  const series: Record<string, number> = {};
  const now = Date.now();
  for (let i = 0; i < 288; i++) {
    const time = new Date(now - (287 - i) * 5 * 60 * 1000).toISOString();
    series[time] = value;
  }
  return series;
}

const usages = {
  '0': makeSeries(20 * GiB),
  '1': makeSeries(25 * GiB),
  '2': makeSeries(18 * GiB),
};

const available = {
  '0': makeSeries(50 * GiB),
  '1': makeSeries(50 * GiB),
  '2': makeSeries(50 * GiB),
};

const meta = {
  component: ChartDiskUsage,
  args: {
    usages,
    available,
    duration: DurationOptions.Last24hours,
  },
} satisfies Meta<typeof ChartDiskUsage>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SingleNode: Story = {
  args: {
    usages: { '0': usages['0'] },
    available: { '0': available['0'] },
  },
};

export const WithoutThresholds: Story = {
  args: { available: {} },
};

export const NoData: Story = {
  args: { usages: {}, available: {} },
};
