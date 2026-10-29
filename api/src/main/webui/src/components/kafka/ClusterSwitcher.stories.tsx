import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter } from 'react-router';
import { userEvent, within } from 'storybook/test'; // or '@storybook/test', matching your other stories
import type { KafkaCluster } from '@/api/types';
import { ClusterSwitcher } from './ClusterSwitcher';

const makeCluster = (id: string, name: string, namespace?: string) =>
  ({
    id,
    attributes: { name, namespace },
    meta: { managed: true },
  } as unknown as KafkaCluster);

const clusters = [
  makeCluster('cluster-1', 'production-kafka', 'kafka-prod'),
  makeCluster('cluster-2', 'staging-kafka', 'kafka-prod'),
  makeCluster('cluster-3', 'development-kafka', 'kafka-dev'),
  makeCluster('cluster-4', 'local-kafka'), // no namespace, goes under "Not provided"
];

const openMenu = async ({ canvasElement }: { canvasElement: HTMLElement }) => {
  await userEvent.click(within(canvasElement).getByRole('button'));
};

const meta = {
  component: ClusterSwitcher,
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/kafka/cluster-1']}>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    clusters,
    currentClusterId: 'cluster-1',
  },
} satisfies Meta<typeof ClusterSwitcher>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Closed: Story = {};

export const Open: Story = {
  play: openMenu,
};

export const SecondClusterSelected: Story = {
  args: { currentClusterId: 'cluster-3' },
  play: openMenu,
};

export const SingleCluster: Story = {
  args: { clusters: [clusters[0]] },
  play: openMenu,
};

export const NoClusters: Story = {
  args: { clusters: [] },
  play: openMenu,
};

export const FilteredNoResults: Story = {
  play: async (context) => {
    await openMenu(context);
    const search = within(document.body).getByRole('textbox', {
      name: 'Filter by name',
    });
    await userEvent.type(search, 'zzz');
  },
};
