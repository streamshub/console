import type { Meta, StoryObj } from "@storybook/nextjs";
import { TopicsTable } from "./TopicsTable";

const meta: Meta<typeof TopicsTable> = {
  component: TopicsTable,
  args: {
    topics: [],
  },
} as Meta<typeof TopicsTable>;

export default meta;
type Story = StoryObj<typeof TopicsTable>;

export const Default: Story = {
  args: {
    topics: [
      {
        kafkaId: "1",
        kafkaName: "kafka1",
        topicId: "1",
        topicName: "console_datagen_000-a",
      },
      {
        kafkaId: "1",
        kafkaName: "kafka1",
        topicId: "3",
        topicName: "console_datagen_000-b",
      },
      {
        kafkaId: "2",
        kafkaName: "kafka2",
        topicId: "1",
        topicName: "__consumer_offsets",
      },
      {
        kafkaId: "2",
        kafkaName: "kafka2",
        topicId: "4",
        topicName: "console_datagen_002-a",
      },
      {
        kafkaId: "2",
        kafkaName: "kafka2",
        topicId: "6",
        topicName: "console_datagen_002-b",
      },
    ],
  },
};
