import { Meta, StoryObj } from "@storybook/nextjs";
import { Dryrun } from "./Dryrun";

export default {
  component: Dryrun,
  args: {},
} as Meta<typeof Dryrun>;

type Story = StoryObj<typeof Dryrun>;

const NewOffsetValues = [
  { topicName: "Topic A", partition: 2, offset: 765, topicId: "cswerqts" },
  { topicName: "Topic C", partition: 2, offset: 121314, topicId: "xcsdwer" },
  { topicName: "Topic A", partition: 0, offset: 1234, topicId: "cswerqts" },
  { topicName: "Topic A", partition: 1, offset: 5678, topicId: "cswerqts" },
  { topicName: "Topic B", partition: 0, offset: 91011, topicId: "weqrests" },
  { topicName: "Topic C", partition: 1, offset: 221222, topicId: "xcsdwer" },
  { topicName: "Topic B", partition: 1, offset: 2341233, topicId: "weqrests" },
  { topicName: "Topic B", partition: 1, offset: 675, topicId: "weqrests" },
];

export const Default: Story = {
  args: {
    consumerGroupName: "console_datagen_002-a",
    newOffset: NewOffsetValues,
    cliCommand: `$ kafka-consumer-groups --bootstrap-server \${bootstrap-Server} --group --reset-offsets`,
  },
};

const DeletedOffsets = [
  { topicName: "Topic A", partition: 0, offset: null },
  { topicName: "Topic A", partition: 1, offset: 5678 },
  { topicName: "Topic B", partition: 0, offset: null },
  { topicName: "Topic C", partition: 1, offset: 221222 },
];

export const DeletedOffsetsStory: Story = {
  args: {
    consumerGroupName: "console_datagen_002-a",
    newOffset: DeletedOffsets,
    cliCommand:
      "$ kafka-consumer-groups --bootstrap-server ${bootstrap-server} --group --reset-offsets",
  },
};
