import { Meta, StoryObj } from "@storybook/react";
import { TypeaheadSelect } from "./TypeaheadSelect";

export default {
  component: TypeaheadSelect,
} as Meta<typeof TypeaheadSelect>;

type Story = StoryObj<typeof TypeaheadSelect>;

export const Default: Story = {
  args: {
    selectItems: ["console_datagen_002-a", "console_datagen_002-b", "console_datagen_002-c", "console_datagen_002-d", "console_datagen_002-a", "console_datagen_002-a", "console_datagen_002-b", "console_datagen_002-c", "console_datagen_002-d", "console_datagen_002-c"],
  },
};
