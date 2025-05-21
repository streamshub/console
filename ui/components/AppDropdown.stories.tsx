// AppDropdown.stories.tsx

import type { Meta, StoryObj } from "@storybook/react";
import { AppDropdown } from "./AppDropdown";

const kafkaClusters = [
  "dev-cluster",
  "prod-cluster",
  "staging-cluster",
  "test-cluster",
  "cluster-01",
  "cluster-02",
];

const meta: Meta<typeof AppDropdown> = {
  component: AppDropdown,
};

export default meta;
type Story = StoryObj<typeof AppDropdown>;

export const Default: Story = {
  args: {
    clusters: [
      {
        projectName: "Project A",
        clusterName: "dev-cluster",
        authenticationMethod: "SSO",
        id: "1",
      },
      {
        projectName: "Project A",
        clusterName: "staging-cluster",
        authenticationMethod: "SSO",
        id: "2",
      },
      {
        projectName: "Project B",
        clusterName: "prod-cluster",
        authenticationMethod: "Basic Auth",
        id: "3",
      },
      {
        projectName: "Project C",
        clusterName: "test-cluster",
        authenticationMethod: "SSO",
        id: "4",
      },
    ],
  },
};
