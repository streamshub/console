import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { expect, userEvent, within } from 'storybook/test';
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

const sortableConfigs = {
  'd.static': {
    type: 'STRING',
    value: '3',
    source: 'STATIC_BROKER_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'b.default': {
    type: 'STRING',
    value: '4',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'c.dynamic': {
    type: 'STRING',
    value: '1',
    source: 'DYNAMIC_GROUP_CONFIG',
    sensitive: false,
    readOnly: false,
  },
  'a.default': {
    type: 'STRING',
    value: '2',
    source: 'DEFAULT_CONFIG',
    sensitive: false,
    readOnly: false,
  },
};

function propertyNames(canvasElement: HTMLElement): string[] {
  return Array.from(canvasElement.querySelectorAll('tbody tr')).map((row) =>
    row.querySelector('td')!.firstElementChild!.textContent!.trim()
  );
}

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

export const SortBySourceUsesSourceValues: Story = {
  decorators: [],
  parameters: {},
  render: () => {
    const Wrapped = withGroup(sortableConfigs)(GroupConfigurationTab);
    return Wrapped;
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const sourceHeader = await canvas.findByRole('columnheader', {
      name: /source/i,
    });
    const propertyHeader = canvas.getByRole('columnheader', {
      name: /property/i,
    });

    await userEvent.click(within(sourceHeader).getByRole('button'));
    expect(sourceHeader).toHaveAttribute('aria-sort', 'ascending');
    expect(propertyHeader).not.toHaveAttribute('aria-sort', 'ascending');
    expect(propertyNames(canvasElement)).toEqual([
      'a.default',
      'b.default',
      'c.dynamic',
      'd.static',
    ]);

    await userEvent.click(within(sourceHeader).getByRole('button'));
    expect(sourceHeader).toHaveAttribute('aria-sort', 'descending');
    expect(propertyNames(canvasElement)).toEqual([
      'd.static',
      'c.dynamic',
      'b.default',
      'a.default',
    ]);
  },
};
