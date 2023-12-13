import type { Meta, StoryObj } from "@storybook/react";
import { composeStories } from "@storybook/react";
import * as ClusterCardStories from "./ClusterCard.stories";
import { PageLayout } from "./PageLayout";
import * as TopicsPartitionsCardStories from "./TopicsPartitionsCard.stories";

const { WithData: ClusterWithData } = composeStories(ClusterCardStories);
const { WithData: TopicsPartitionsWithData } = composeStories(
  TopicsPartitionsCardStories,
);

const meta: Meta<typeof PageLayout> = {
  component: PageLayout,
};

export default meta;
type Story = StoryObj<typeof PageLayout>;

export const WithData: Story = {
  render: (args) => (
    <PageLayout
      {...args}
      clusterOverview={<ClusterWithData {...ClusterWithData.args} />}
      clusterCharts={<div>todo</div>}
      topicsPartitions={
        <TopicsPartitionsWithData {...TopicsPartitionsWithData.args} />
      }
    />
  ),
};
export const Loading: Story = {
  render: (args) => (
    <PageLayout
      {...args}
      clusterOverview={<Everloading />}
      clusterCharts={<Everloading />}
      topicsPartitions={<Everloading />}
    />
  ),
};

async function Everloading() {
  await new Promise(() => {});
  return <div>hello</div>;
}
