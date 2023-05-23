import type { ComponentStory, ComponentMeta } from "@storybook/react";
import { Pagination } from "./Pagination";

export default {
  component: Pagination,
  args: {
    itemCount: 500,
    page: 1,
    perPage: 10,
  },
} as ComponentMeta<typeof Pagination>;

const Template: ComponentStory<typeof Pagination> = (args) => (
  <Pagination {...args} />
);

export const DefaultPagination = Template.bind({});

export const CompactPagination = Template.bind({});
CompactPagination.args = {
  isCompact: true,
};
