import type { Meta, StoryObj } from "@storybook/nextjs";
import { FilterByTime } from "./FilterByTime";

const meta: Meta<typeof FilterByTime> = {
  component: FilterByTime,
  args: {
    keyText: "string",
    disableToolbar: false,
    ariaLabel: "the aria label",
    duration: 60,
  },
} as Meta<typeof FilterByTime>;

export default meta;
type Story = StoryObj<typeof FilterByTime>;

export const Default: Story = {};
