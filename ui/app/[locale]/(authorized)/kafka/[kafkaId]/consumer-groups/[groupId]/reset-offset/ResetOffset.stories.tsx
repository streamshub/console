import { Meta, StoryObj } from "@storybook/nextjs";
import { ResetOffset } from "./ResetOffset";

export default {
  component: ResetOffset,
} as Meta<typeof ResetOffset>;

type Story = StoryObj<typeof ResetOffset>;

export const AllTopicsLatest: Story = {
  args: {
    topics: [
      { topicId: "1", topicName: "topic-a" },
      { topicId: "2", topicName: "topic-b" },
    ],
    partitions: [{ topicId: "1", partitionNumber: 0 }],
    selectTopic: "allTopics",
    selectPartition: "allPartitions",
    selectOffset: "latest",
    offset: { topicId: "", topicName: "", partition: 0, offset: "" },
    isLoading: false,
    cliCommand: "kafka reset offset example",
  },
};

export const CustomOffset: Story = {
  args: {
    topics: [
      { topicId: "1", topicName: "orders" },
      { topicId: "2", topicName: "payments" },
    ],
    partitions: [
      { topicId: "1", partitionNumber: 0 },
      { topicId: "1", partitionNumber: 1 },
    ],
    selectTopic: "selectedTopic",
    selectPartition: "selectedPartition",
    selectOffset: "custom",
    offset: {
      topicId: "1",
      topicName: "orders",
      partition: 1,
      offset: 50,
    },
    isLoading: false,
    cliCommand: "kafka reset offset custom example",
  },
};

export const DeleteOffset: Story = {
  args: {
    topics: [{ topicId: "10", topicName: "inventory" }],
    partitions: [
      { topicId: "10", partitionNumber: 0 },
      { topicId: "10", partitionNumber: 1 },
    ],
    selectTopic: "selectedTopic",
    selectPartition: "allPartitions",
    selectOffset: "delete",
    offset: {
      topicId: "10",
      topicName: "inventory",
      partition: 0,
      offset: "",
    },
    isLoading: false,
    cliCommand: "kafka delete offset example",
  },
};

export const SpecificDateISO: Story = {
  args: {
    topics: [{ topicId: "1", topicName: "logs" }],
    partitions: [{ topicId: "1", partitionNumber: 0 }],
    selectTopic: "selectedTopic",
    selectPartition: "selectedPartition",
    selectOffset: "specificDateTime",
    selectDateTimeFormat: "ISO",
    offset: {
      topicId: "1",
      topicName: "logs",
      partition: 0,
      offset: "",
    },
    cliCommand: "kafka reset offset ISO",
  },
};

export const SpecificDateEpoch: Story = {
  args: {
    topics: [{ topicId: "1", topicName: "logs" }],
    partitions: [{ topicId: "1", partitionNumber: 0 }],
    selectTopic: "selectedTopic",
    selectPartition: "selectedPartition",
    selectOffset: "specificDateTime",
    selectDateTimeFormat: "Epoch",
    offset: {
      topicId: "1",
      topicName: "logs",
      partition: 0,
      offset: "",
    },
    cliCommand: "kafka reset offset Epoch",
  },
};

export const WithErrors: Story = {
  args: {
    topics: [{ topicId: "2", topicName: "errors-topic" }],
    partitions: [{ topicId: "2", partitionNumber: 0 }],
    selectTopic: "selectedTopic",
    selectPartition: "selectedPartition",
    selectOffset: "custom",
    offset: {
      topicId: "2",
      topicName: "errors-topic",
      partition: 0,
      offset: "",
    },
    error: {
      GeneralError: "Something went wrong while resetting offsets.",
      PartitionError: "Invalid partition selected.",
      CustomOffsetError: "Offset must be a positive number.",
      SpecificDateTimeNotValidError: "",
    },
    cliCommand: "kafka reset error case",
  },
};

export const LoadingState: Story = {
  args: {
    topics: [{ topicId: "7", topicName: "loading-topic" }],
    partitions: [{ topicId: "7", partitionNumber: 0 }],
    selectTopic: "allTopics",
    selectPartition: "allPartitions",
    selectOffset: "latest",
    offset: { topicId: "", topicName: "", partition: 0, offset: 0 },
    isLoading: true,
    cliCommand: "loading example",
  },
};

export const EmptyData: Story = {
  args: {
    topics: [],
    partitions: [],
    selectTopic: "selectedTopic",
    selectPartition: "selectedPartition",
    selectOffset: "custom",
    offset: { topicId: "", topicName: "", partition: 0, offset: "" },
    cliCommand: "",
  },
};
