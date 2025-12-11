import type { Meta, StoryObj } from "@storybook/nextjs";
import { composeStories } from "@storybook/react";
import * as ClusterCardStories from "./ClusterCard.stories";
import { PageLayout } from "./PageLayout";
import * as TopicsPartitionsCardStories from "./TopicsPartitionsCard.stories";
import { ReconciliationContext } from "../ReconciliationContext";

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
    <ReconciliationContext.Provider
      value={{
        isReconciliationPaused: false,
        setReconciliationPaused: () => {},
      }}
    >
      <PageLayout
        {...args}
        clusterOverview={<Everloading />}
        clusterCharts={<Everloading />}
        topicsPartitions={<Everloading />}
      />
    </ReconciliationContext.Provider>
  ),
};

async function Everloading() {
  await new Promise(() => {});
  return <div>hello</div>;
}
