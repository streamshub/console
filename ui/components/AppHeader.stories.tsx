import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  PageNavigation,
} from "@/libs/patternfly/react-core";
import type { Meta, StoryObj } from "@storybook/react";
import { AppHeader } from "./AppHeader";

const meta: Meta<typeof AppHeader> = {
  title: "Components/AppHeader",
  component: AppHeader,
  args: {
    title: "Main Title",
    subTitle: "Sub Title",
  },
  argTypes: {
    title: { control: "text" },
    subTitle: { control: "text" },
    actions: { control: "none" },
    navigation: { control: "none" },
  },
};

export default meta;
type Story = StoryObj<typeof AppHeader>;

export const Default: Story = {};

export const WithActions: Story = {
  args: {
    actions: [
      <Button key="action1">Action 1</Button>,
      <Button key="action2">Action 2</Button>,
    ],
  },
};

export const WithNavigation: Story = {
  args: {
    navigation: (
      <PageNavigation>
        <Breadcrumb>
          <BreadcrumbItem to="#">Home</BreadcrumbItem>
          <BreadcrumbItem to="#">Section</BreadcrumbItem>
          <BreadcrumbItem to="#" isActive>
            Page
          </BreadcrumbItem>
        </Breadcrumb>
      </PageNavigation>
    ),
  },
};

export const FullFeatured: Story = {
  args: {
    actions: [
      <Button key="action1">Action 1</Button>,
      <Button key="action2">Action 2</Button>,
    ],
    navigation: (
      <PageNavigation>
        <Breadcrumb>
          <BreadcrumbItem to="#">Home</BreadcrumbItem>
          <BreadcrumbItem to="#">Section</BreadcrumbItem>
          <BreadcrumbItem to="#" isActive>
            Page
          </BreadcrumbItem>
        </Breadcrumb>
      </PageNavigation>
    ),
  },
};
