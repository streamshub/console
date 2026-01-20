import type { Meta, StoryObj } from "@storybook/nextjs";
import { FilterByTopic } from "./FilterByTopic";

const topics = (names: string[]) => names.map((name) => ({ id: name, name }));

const meta: Meta<typeof FilterByTopic> = {
  component: FilterByTopic,
  args: {
    selectedTopic: undefined,
    topicList: topics(["lorem", "dolor", "ipsum"]),
    disableToolbar: false,
  },
} as Meta<typeof FilterByTopic>;

export default meta;
type Story = StoryObj<typeof FilterByTopic>;

export const Default: Story = {};
Default.args = {};

export const Disabled: Story = {};
Disabled.args = {
  disableToolbar: true,
  selectedTopic: "lorem",
};

export const NoTopics: Story = {};
NoTopics.args = {
  topicList: [],
};

export const MultipleTopicsWithCommonWords: Story = {};
MultipleTopicsWithCommonWords.args = {
  topicList: topics([
    "lorem dolor",
    "lorem ipsum",
    "lorem foo",
    "dolor",
    "ipsum",
  ]),
};

export const DoesNotBreakWithLongWords: Story = {};
DoesNotBreakWithLongWords.args = {
  topicList: topics([
    "lorem dolor lorem dolor lorem dolor lorem dolor lorem dolor lorem dolor",
    "lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum",
    "lorem foo",
    "dolor",
    "ipsum",
  ]),
};
