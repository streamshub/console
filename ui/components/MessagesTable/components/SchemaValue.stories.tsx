import { Meta, StoryObj } from "@storybook/react";
import { schemaValue } from "./SchemaValue";

export default {
  component: schemaValue,
  args: {},
} as Meta<typeof schemaValue>;

type Story = StoryObj<typeof schemaValue>;

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
