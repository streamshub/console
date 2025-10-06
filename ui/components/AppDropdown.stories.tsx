import type { Meta, StoryObj } from "@storybook/nextjs";
import { AppDropdown, ClusterInfo } from "./AppDropdown";

const clusters: ClusterInfo[] = [
  {
    projectName: "Project A",
    clusterName: "dev-cluster",
    authenticationMethod: "SSO",
    id: "1",
    loginRequired: true,
  },
  {
    projectName: "Project A",
    clusterName: "staging-cluster",
    authenticationMethod: "SSO",
    id: "2",
    loginRequired: false,
  },
  {
    projectName: "Project B",
    clusterName: "prod-cluster",
    authenticationMethod: "Basic Auth",
    id: "3",
    loginRequired: true,
  },
  {
    projectName: "Project C",
    clusterName: "test-cluster",
    authenticationMethod: "SSO",
    id: "4",
    loginRequired: false,
  },
];

const meta: Meta<typeof AppDropdown> = {
  component: AppDropdown,
  argTypes: {
    isOidcEnabled: { control: "boolean" },
  },
};

export default meta;
type Story = StoryObj<typeof AppDropdown>;

export const Default: Story = {
  args: {
    clusters,
    kafkaId: "1",
    isOidcEnabled: false,
  },
};

export const OIDCEnabled: Story = {
  args: {
    clusters,
    kafkaId: "1",
    isOidcEnabled: true,
  },
};
