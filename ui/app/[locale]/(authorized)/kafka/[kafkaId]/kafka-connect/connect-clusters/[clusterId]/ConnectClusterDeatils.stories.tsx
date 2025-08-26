import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectClusterDetails } from "./ConnectClusterDetails";

const meta: Meta<typeof ConnectClusterDetails> = {
  component: ConnectClusterDetails,
  args: {
    connectVersion: "4.0.0",
    workers: 3,
    data: [
      {
        id: "11234",
        name: "my-connector-cluster",
        type: "sink",
        state: "RUNNING",
        replicas: 2,
      },
      {
        id: "324dxsgs",
        name: "another-connector",
        type: "source",
        state: "PAUSED",
        replicas: 1,
      },
    ],
  },
};

export default meta;
type Story = StoryObj<typeof ConnectClusterDetails>;

export const Default: Story = {};
