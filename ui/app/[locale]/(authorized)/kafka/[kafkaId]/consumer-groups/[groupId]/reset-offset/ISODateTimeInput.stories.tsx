import { Meta, StoryObj } from "@storybook/nextjs";
import { ISODateTimeInput } from "./ISODateTimeInput";
import { useState } from "react";
import { fn } from "storybook/test";

export default {
  component: ISODateTimeInput,
} as Meta<typeof ISODateTimeInput>;

type Story = StoryObj<typeof ISODateTimeInput>;

const ControlledTemplate: Story = {
  render: (args) => {
    const [value, setValue] = useState(args.value ?? "");

    return (
      <ISODateTimeInput {...args} value={value} onValidChange={setValue} />
    );
  },
  args: {
    onValidityChange: fn(),
  },
};

export const WithUTCValue: Story = {
  ...ControlledTemplate,
  args: {
    ...ControlledTemplate.args,
    value: "2023-11-02T22:37:22.000Z",
  },
};

export const WithLocalValue: Story = {
  ...ControlledTemplate,
  args: {
    ...ControlledTemplate.args,
    value: "2023-11-02T18:37:22-04:00",
  },
};

export const ErrorState: Story = {
  ...ControlledTemplate,
  args: {
    ...ControlledTemplate.args,
    value: "invalid-date-string",
  },
};
