import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectClusterDetails } from "./ConnectClusterDetails";

const meta: Meta<typeof ConnectClusterDetails> = {
  component: ConnectClusterDetails,
  args: {
    connectVersion: "4.0.0",
    workers: 3,
  },
};

export default meta;
type Story = StoryObj<typeof ConnectClusterDetails>;

export const Default: Story = {};
