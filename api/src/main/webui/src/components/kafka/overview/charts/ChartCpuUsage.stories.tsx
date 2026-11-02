import type { Meta, StoryObj } from '@storybook/react';
import { ChartCpuUsage } from './ChartCpuUsage';
import {
  DurationOptions,
  TimeSeriesMetrics,
} from '@/components/kafka/overview/utils/types';

const mockUsages: Record<string, TimeSeriesMetrics> = {
  '0': {
    '2026-10-29T09:00:00Z': 0.25,
    '2026-10-29T09:05:00Z': 0.35,
    '2026-10-29T09:10:00Z': 0.3,
    '2026-10-29T09:15:00Z': 0.45,
    '2026-10-29T09:20:00Z': 0.4,
  },
  '1': {
    '2026-10-29T09:00:00Z': 0.15,
    '2026-10-29T09:05:00Z': 0.2,
    '2026-10-29T09:10:00Z': 0.25,
    '2026-10-29T09:15:00Z': 0.3,
    '2026-10-29T09:20:00Z': 0.2,
  },
};

const meta = {
  component: ChartCpuUsage,
  args: {
    usages: mockUsages,
    duration: DurationOptions.Last1hour,
  },
} satisfies Meta<typeof ChartCpuUsage>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const NoData: Story = {
  args: {
    usages: {},
  },
};

export const Last24Hours: Story = {
  args: {
    duration: DurationOptions.Last24hours,
  },
};
