import type { Meta, StoryObj } from "@storybook/nextjs";
import { FilterByBroker } from "./FilterByBroker";

const meta: Meta<typeof FilterByBroker> = {
  component: FilterByBroker,
  args: {
    selectedBroker: "Broker1",
    brokerList: ["Broker1", "Broker2", "Broker3"],
  },
} as Meta<typeof FilterByBroker>;

export default meta;
type Story = StoryObj<typeof FilterByBroker>;

export const Default: Story = {};
