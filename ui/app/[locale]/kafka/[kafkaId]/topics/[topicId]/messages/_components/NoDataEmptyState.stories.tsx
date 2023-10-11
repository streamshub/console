import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { NoDataEmptyState } from "./NoDataEmptyState";

export default {
  component: NoDataEmptyState,
} as ComponentMeta<typeof NoDataEmptyState>;

const Template: ComponentStory<typeof NoDataEmptyState> = (args) => (
  <NoDataEmptyState {...args}>todo</NoDataEmptyState>
);

export const Example = Template.bind({});
Example.args = {};
