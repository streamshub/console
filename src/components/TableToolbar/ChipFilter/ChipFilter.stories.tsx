import { Toolbar, ToolbarContent } from "@patternfly/react-core";
import type { ComponentMeta, ComponentStory } from "@storybook/react";

import { ChipFilter } from "./ChipFilter";
import {
  sampleCheckboxFilter,
  sampleCheckboxFilterWithChips,
  sampleSearchFilter,
  sampleSearchFilterWithChips,
} from "./storybookHelpers";

export default {
  component: ChipFilter,
  args: {
    breakpoint: "md",
    filters: {
      Name: sampleSearchFilter,
      Options: sampleCheckboxFilter,
    },
  },
} as ComponentMeta<typeof ChipFilter>;

const Template: ComponentStory<typeof ChipFilter> = (args) => (
  <Toolbar
    clearAllFilters={() => {
      /* noop */
    }}
  >
    <ToolbarContent>
      <ChipFilter {...args} />
    </ToolbarContent>
  </Toolbar>
);

export const DesktopMultipleFilters = Template.bind({});

export const DesktopMultipleFiltersWithChips = Template.bind({});
DesktopMultipleFiltersWithChips.args = {
  filters: {
    Name: sampleSearchFilterWithChips,
    Options: sampleCheckboxFilterWithChips,
  },
};

export const MobileMultipleFilters = Template.bind({});
MobileMultipleFilters.parameters = {
  viewport: {
    defaultViewport: "xs",
  },
};

export const MobileMultipleFiltersWithChips = Template.bind({});
MobileMultipleFiltersWithChips.args = {
  filters: {
    Name: sampleSearchFilterWithChips,
    Options: sampleCheckboxFilterWithChips,
  },
};
MobileMultipleFiltersWithChips.parameters = {
  viewport: {
    defaultViewport: "xs",
  },
};

export const DesktopSingleFilter = Template.bind({});
DesktopSingleFilter.args = {
  filters: {
    Name: sampleSearchFilter,
  },
};

export const DesktopSingleFilterWithChips = Template.bind({});
DesktopSingleFilterWithChips.args = {
  filters: {
    Name: sampleSearchFilterWithChips,
  },
};

export const MobileSingleFilter = Template.bind({});
MobileSingleFilter.args = {
  filters: {
    Name: sampleSearchFilter,
  },
};
MobileSingleFilter.parameters = {
  viewport: {
    defaultViewport: "xs",
  },
};

export const MobileSingleFilterWithChips = Template.bind({});
MobileSingleFilterWithChips.args = {
  filters: {
    Name: sampleSearchFilterWithChips,
  },
};
MobileSingleFilterWithChips.parameters = {
  viewport: {
    defaultViewport: "xs",
  },
};
