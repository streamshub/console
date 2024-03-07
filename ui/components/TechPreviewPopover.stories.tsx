import { HelpIcon } from "@patternfly/react-icons";
import type { Meta, StoryObj } from "@storybook/react";

import { TechPreviewPopover as Comp } from "./TechPreviewPopover";

export default {
  component: Comp,
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

export const TechPreviewPopover: Story = {
  args: {
    children: <HelpIcon />,
  },
};
