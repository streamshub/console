import type { Meta, StoryObj } from "@storybook/react";
import { FieldName } from "./FieldName";

const meta: Meta<typeof FieldName> = {
  component: FieldName,
};

export default meta;
type Story = StoryObj<typeof FieldName>;

export const ValidName: Story = {
  args: {
    name: "Hello world"
  },
};

export const InvalidName: Story = {
  args: {
    name: "..",
    nameInvalid: true
  },
};

export const InvalidLength: Story = {
  args: {
    name: "..",
    nameInvalid: true,
    lengthInvalid: true
  },
};

export const NameFormatInvalid: Story = {
  args: {
    name: "123",
    formatInvalid: true
  },
};

