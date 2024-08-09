import { Meta, StoryObj } from "@storybook/react";
import { ResetOffsetModal } from "./ResetOffsetModal";

export default {
  component: ResetOffsetModal,
} as Meta<typeof ResetOffsetModal>;

type Story = StoryObj<typeof ResetOffsetModal>;

export const Default: Story = {
  args: {
    consumerGroupId: "console-datagen-group-0",
    members: ["console-datagen-consumer-0", "console-datagen-consumer-1"]
  },
};
