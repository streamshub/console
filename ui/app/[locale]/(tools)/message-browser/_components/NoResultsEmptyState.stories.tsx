import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { NoResultsEmptyState } from "./NoResultsEmptyState";

export default {
  component: NoResultsEmptyState,
} as ComponentMeta<typeof NoResultsEmptyState>;

const Template: ComponentStory<typeof NoResultsEmptyState> = (args) => (
  <NoResultsEmptyState {...args}>todo</NoResultsEmptyState>
);

export const Example = Template.bind({});
Example.args = {};
