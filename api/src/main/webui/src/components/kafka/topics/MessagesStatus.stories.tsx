import type { Meta, StoryObj } from '@storybook/react';
import { expect, fn, userEvent, waitFor, within } from 'storybook/test';
import type { ScanState } from '@/api/hooks/useMessages';
import { MessagesStatus } from './MessagesStatus';

const running: ScanState = {
  scanned: 12345,
  progress: 40,
  elapsed: 12_000,
  remaining: 288_000,
};

const finished = (endReason: ScanState['endReason']): ScanState => ({
  scanned: 98765,
  progress: endReason === 'complete' ? 100 : 62,
  elapsed: 83_000,
  remaining: 217_000,
  endReason,
});

const meta = {
  component: MessagesStatus,
  args: {
    count: 50,
    scan: { scanned: 0, progress: 0 },
    isSearching: false,
    isStreaming: false,
    isStopped: false,
    onCancel: fn(),
  },
} satisfies Meta<typeof MessagesStatus>;

export default meta;
type Story = StoryObj<typeof meta>;

/* Retrieving without a search term */

export const RetrievingNoTimeYet: Story = {
  args: { count: 0, isStreaming: true },
};

export const Retrieving: Story = {
  args: {
    count: 20,
    isStreaming: true,
    scan: { ...running, scanned: 20 },
  },
};

export const RetrievalFinished: Story = {
  args: {
    scan: { scanned: 50, progress: 100, elapsed: 1_400, remaining: 298_600, endReason: 'complete' },
  },
};

/* Searching */

export const SearchingNoTimeYet: Story = {
  args: { count: 0, isSearching: true, isStreaming: true },
};

export const Searching: Story = {
  args: {
    count: 3,
    isSearching: true,
    isStreaming: true,
    scan: running,
  },
};

export const SearchingTimeNearlyUp: Story = {
  args: {
    count: 3,
    isSearching: true,
    isStreaming: true,
    scan: { ...running, elapsed: 299_000, remaining: 1_000 },
  },
};

export const SearchComplete: Story = {
  args: {
    count: 7,
    isSearching: true,
    scan: finished('complete'),
  },
};

export const SearchFoundAllRequestedMatches: Story = {
  args: {
    count: 50,
    isSearching: true,
    scan: finished('limit'),
  },
};

export const SearchTimedOut: Story = {
  args: {
    count: 2,
    isSearching: true,
    scan: { ...finished('timeout'), elapsed: 300_000, remaining: 0 },
  },
};

export const SearchEndedByServer: Story = {
  args: {
    count: 2,
    isSearching: true,
    scan: finished('cancelled'),
  },
};

export const SearchStoppedByUser: Story = {
  args: {
    count: 2,
    isSearching: true,
    isStopped: true,
    scan: { ...running, scanned: 4000 },
  },
};

export const SearchWithoutMatches: Story = {
  args: {
    count: 0,
    isSearching: true,
    scan: finished('complete'),
  },
};

/* Formatting */

export const LongRunningHoursFormat: Story = {
  args: {
    count: 1,
    isSearching: true,
    isStreaming: true,
    scan: { ...running, elapsed: 3_725_000, remaining: 3_475_000 },
  },
};

/* Interactions */

export const TimeTooltip: Story = {
  args: {
    count: 3,
    isSearching: true,
    isStreaming: true,
    scan: running,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    const time = canvas.getByText('0:12 elapsed, 4:48 left');
    await expect(time).toBeInTheDocument();

    const trigger = time.closest<HTMLElement>('[tabindex="0"]')!;
    const tooltip = () =>
      within(document.body).getByText(/The maximum time is configured/);

    // PatternFly attaches the tooltip's focus listener in an effect that runs
    // after the first render, so focusing right away can be missed. Focus again
    // until the tooltip opens. Focus is also how keyboard users reach the
    // tooltip, and unlike hover it does not depend on the pointer position.
    // The tooltip is rendered outside of the story canvas.
    let opened = false;
    for (let attempt = 0; attempt < 5 && !opened; attempt++) {
      trigger.blur();
      trigger.focus();
      try {
        await waitFor(() => expect(tooltip()).toBeVisible(), { timeout: 1000 });
        opened = true;
      } catch {
        // Listener not attached yet, try again
      }
    }

    await expect(tooltip()).toBeVisible();
  },
};

export const CancelWhileSearching: Story = {
  args: {
    count: 3,
    isSearching: true,
    isStreaming: true,
    scan: running,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByRole('button', { name: 'Cancel retrieving messages' }));
    await expect(args.onCancel).toHaveBeenCalledTimes(1);
  },
};

export const DurationShownWhenFinished: Story = {
  args: {
    count: 7,
    isSearching: true,
    scan: finished('complete'),
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText('7 matches from 98,765 scanned in 1:23')).toBeInTheDocument();
    await expect(canvas.getByText('Scan complete')).toBeInTheDocument();
  },
};

export const NoDurationWithoutHeartbeat: Story = {
  args: {
    count: 5,
    isStreaming: false,
    scan: { scanned: 0, progress: 0 },
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText('5 messages')).toBeInTheDocument();
    await expect(canvas.queryByText(/ in \d+:\d\d/)).not.toBeInTheDocument();
  },
};
