import { Meta, StoryObj } from "@storybook/react";
import { ResetConsumerOffset } from "./ResetConsumerOffset";

export default {
  component: ResetConsumerOffset,
} as Meta<typeof ResetConsumerOffset>;

type Story = StoryObj<typeof ResetConsumerOffset>;

export const Default: Story = {
  args: {
    consumerGroupName: "console-consumer-01",
    topics: [
      { topicId: "123", topicName: "console_datagen_002-a" },
      { topicId: "456", topicName: "console_datagen_002-b" },
      { topicId: "234", topicName: "console_datagen_002-c" },
      { topicId: "431", topicName: "console_datagen_002-d" },
    ],
    partitions: [1, 2, 3],
  },
};
