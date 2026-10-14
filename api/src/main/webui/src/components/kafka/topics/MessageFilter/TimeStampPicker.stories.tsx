import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, userEvent, within } from 'storybook/test'; // '@storybook/test' on Storybook 8
import { TimestampPicker } from './TimeStampPicker';

const meta = {
  component: TimestampPicker,
  parameters: { layout: 'padded' },
  decorators: [
    // The calendar opens below the input, so leave room for it
    (Story) => (
      <div style={{ minHeight: 520, maxWidth: 420 }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof TimestampPicker>;

export default meta;
type Story = StoryObj<typeof meta>;

/** Keeps the value in state like the real filter does, and shows what was committed. */
function Controlled({ initial }: { initial?: string }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <TimestampPicker value={value} onCommit={setValue} />
      <pre style={{ marginTop: 16 }}>committed: {value ?? '(none)'}</pre>
    </>
  );
}

export const Empty: Story = {
  args: { onCommit: () => {} },
  render: () => <Controlled />,
};

export const WithValue: Story = {
  args: { onCommit: () => {} },
  render: () => <Controlled initial="2026-10-14T09:30:00.000+00:00" />,
};

export const InvalidText: Story = {
  args: { onCommit: () => {} },
  render: () => <Controlled initial="not-a-timestamp" />,
};

/** Opens the popover automatically so you can see the calendar without clicking. */
export const PopoverOpen: Story = {
  args: { onCommit: () => {} },
  render: () => <Controlled initial="2026-10-14T09:30:00.000+00:00" />,
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getByRole('button', { name: 'Open timestamp picker' })
    );
    // The popper is appended to document.body, not the story canvas
    await expect(
      within(document.body).getByRole('button', { name: 'Confirm' })
    ).toBeVisible();
  },
};

/** Opens, types a time and confirms, so the committed value updates. */
export const ConfirmFlow: Story = {
  args: { onCommit: () => {} },
  render: () => <Controlled />,
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const body = within(document.body);
    await userEvent.click(
      canvas.getByRole('button', { name: 'Open timestamp picker' })
    );
    const time = await body.findByLabelText('Time');
    await userEvent.clear(time);
    await userEvent.type(time, '14:30:00.000');
    await userEvent.click(body.getByRole('button', { name: 'Confirm' }));
  },
};
