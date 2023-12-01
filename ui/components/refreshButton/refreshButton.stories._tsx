import type { ComponentMeta, ComponentStory } from "@storybook/react";

import { RefreshButton } from "./refreshButton";

export default {
  component: RefreshButton,
  args: {},
} as ComponentMeta<typeof RefreshButton>;

const Template: ComponentStory<typeof RefreshButton> = (args) => (
  <RefreshButton {...args} />
);

export const Default = Template.bind({});
Default.args = {
  tooltip: "Reload contents",
};

export const Refreshing = Template.bind({});
Refreshing.args = {
  isRefreshing: true,
  tooltip: "Data is currently refreshing",
};

export const Disabled = Template.bind({});
Disabled.args = {
  isDisabled: true,
};
