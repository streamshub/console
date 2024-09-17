import { Meta, StoryObj } from "@storybook/react";
import { DryrunSelect } from "./DryrunSelect";

export default {
  component: DryrunSelect,
  args: {},
} as Meta<typeof DryrunSelect>;

type Story = StoryObj<typeof DryrunSelect>;

export const Default: Story = {
  args: {
    cliCommand:
      "$kafka-consumer-groups --bootstrap-server localhost:9092 --group my-consumer-group --reset-offsets --topic mytopic --to-earliest --dry-run",
  },
};
