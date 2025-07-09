import { Meta, StoryObj } from "@storybook/nextjs";
import { ResetOffsetModal } from "./ResetOffsetModal";

export default {
  component: ResetOffsetModal,
} as Meta<typeof ResetOffsetModal>;

type Story = StoryObj<typeof ResetOffsetModal>;

export const Default: Story = {
  args: {
    isResetOffsetModalOpen: true,
    members: ["console-datagen-consumer-0", "console-datagen-consumer-1"],
  },
};
