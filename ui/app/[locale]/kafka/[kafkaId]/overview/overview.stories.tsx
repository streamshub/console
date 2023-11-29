import type { Meta, StoryObj } from "@storybook/react";

import OverviewPage from "./page";

const meta: Meta<typeof OverviewPage> = {
  component: OverviewPage,
};

export default meta;
type Story = StoryObj<typeof OverviewPage>;

export const Default: Story = {
  render: () => <OverviewPage params={{ kafkaId: "123" }} />,
};
