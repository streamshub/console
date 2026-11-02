import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PageSection } from '@patternfly/react-core';
import { GroupDetailPageView } from './GroupDetailPage';
import type { Group } from '@/api/types';

// Static mock group (cast so you don't have to fill every field of Group)
const group = {
  id: 'group-1',
  type: 'groups',
  attributes: {
    groupId: 'orders-consumer-group',
    state: 'EMPTY',
    protocol: 'consumer',
  },
} as unknown as Group;

const meta = {
  component: GroupDetailPageView,
  parameters: { layout: 'fullscreen' },
  decorators: [
    // ResetOffsetModal (rendered closed by the page) may use React Query hooks
    (Story) => (
      <QueryClientProvider client={new QueryClient()}>
        <Story />
      </QueryClientProvider>
    ),
  ],
  args: {
    kafkaId: 'kafka-1',
    groupName: 'orders-consumer-group',
    group,
    isLoading: false,
    error: null,
    canResetOffset: true,
    activeTab: 'members',
    onTabSelect: () => {},
    // Placeholder for the selected tab's content (the router <Outlet /> in the real page)
    children: <PageSection>Selected tab content goes here</PageSection>,
  },
} satisfies Meta<typeof GroupDetailPageView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

// Reset offset button is disabled (group not empty, or no UPDATE privilege)
export const ResetOffsetDisabled: Story = {
  args: { canResetOffset: false },
};

export const ConfigurationTab: Story = {
  args: { activeTab: 'configuration' },
};

export const Loading: Story = {
  args: { isLoading: true, group: undefined },
};

export const ErrorState: Story = {
  args: { error: new Error('Group not found'), group: undefined },
};

// Keeps the active tab in state, so clicking a tab switches it
export const InteractiveTabs: Story = {
  render: (args) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const [tab, setTab] = useState<string | number>('members');
    return (
      <GroupDetailPageView
        {...args}
        activeTab={String(tab)}
        onTabSelect={setTab}
      />
    );
  },
};
