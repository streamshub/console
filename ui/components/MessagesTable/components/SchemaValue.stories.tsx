import { Meta, StoryObj } from "@storybook/react";
import { SchemaValue } from "./SchemaValue";

export default {
  component: SchemaValue,
  args: {},
} as Meta<typeof SchemaValue>;

type Story = StoryObj<typeof SchemaValue>;

export const Default: Story = {
  args: {
    name: "SchemaValue",
    schema: JSON.stringify({
      type: "record",
      name: "price",
      namespace: "com.example",
      fields: [
        { name: "symbol", type: "string" },
        { name: "price", type: "string" },
      ],
    }),
  },
};
