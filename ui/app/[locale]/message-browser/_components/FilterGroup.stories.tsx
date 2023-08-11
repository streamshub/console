import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { FilterGroup } from "./FilterGroup";

export default {
  component: FilterGroup,
} as ComponentMeta<typeof FilterGroup>;

const Template: ComponentStory<typeof FilterGroup> = (args) => (
  <FilterGroup {...args}>todo</FilterGroup>
);

export const Example = Template.bind({});
Example.args = {};
