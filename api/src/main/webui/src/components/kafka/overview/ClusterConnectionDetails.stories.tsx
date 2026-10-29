import type { Meta, StoryObj } from '@storybook/react';
import { ClusterConnectionDetails } from './ClusterConnectionDetails';
import type { KafkaCluster } from '@/api/types';

const mockCluster = {
  attributes: {
    listeners: [
      {
        type: 'external',
        bootstrapServers: 'kafka.example.com:9094',
        authType: 'TLS',
      },
      {
        type: 'external',
        bootstrapServers: 'kafka-public.example.com:9095',
        authType: 'SASL/SCRAM',
      },
      {
        type: 'internal',
        bootstrapServers: 'kafka.kafka-ns.svc:9092',
        authType: 'none',
      },
    ],
  },
} as KafkaCluster;

const meta = {
  component: ClusterConnectionDetails,
  args: {
    cluster: mockCluster,
    showLearning: false,
  },
} satisfies Meta<typeof ClusterConnectionDetails>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithLearningLinks: Story = {
  args: {
    showLearning: true,
  },
};

export const UndefinedCluster: Story = {
  args: {
    cluster: undefined,
  },
};
