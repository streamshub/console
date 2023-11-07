import type { StoryFn, Meta } from "@storybook/react";
import { Pagination } from "./pagination";

export default {
  component: Pagination,
  args: {
    itemCount: 500,
    page: 1,
    perPage: 10,
  },
} as Meta<typeof Pagination>;

const Template: StoryFn<typeof Pagination> = (args) => <Pagination {...args} />;

export const DefaultPagination = Template.bind({});

export const CompactPagination = Template.bind({});
CompactPagination.args = {
  isCompact: true,
};
