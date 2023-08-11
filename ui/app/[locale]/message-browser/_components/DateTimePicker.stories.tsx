import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { DateTimePicker } from "./DateTimePicker";

export default {
  component: DateTimePicker,
} as ComponentMeta<typeof DateTimePicker>;

const Template: ComponentStory<typeof DateTimePicker> = (args) => (
  <DateTimePicker {...args}>todo</DateTimePicker>
);

export const Example = Template.bind({});
Example.args = {};
