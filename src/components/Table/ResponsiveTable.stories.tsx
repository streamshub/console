import {
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStateVariant,
  Title,
} from "@patternfly/react-core";
import { InfoIcon } from "@patternfly/react-icons";
import { actions } from "@storybook/addon-actions";
import type { ComponentMeta, ComponentStory } from "@storybook/react";
import type { VoidFunctionComponent } from "react";
import type { ResponsiveTableProps } from "./ResponsiveTable";
import { ResponsiveTable } from "./ResponsiveTable";
import type { SampleDataType } from "./storybookHelpers";
import {
  columnLabels,
  columns,
  defaultActions,
  sampleData,
} from "./storybookHelpers";
import { useSortableSearchParams } from "./useSortableSearchParams";

const eventsFromNames = actions("onRowClick");

const ResponsiveTableSampleType: VoidFunctionComponent<
  ResponsiveTableProps<SampleDataType, typeof columns[number]> & {
    hasActions?: boolean;
    hasCustomActionTestId?: boolean;
    hasCustomOuiaIds?: boolean;
    isRowClickable?: boolean;
    isSortable?: boolean;
    sortAllColumns?: boolean;
    selectedRow?: number;
  }
> = (props) => <ResponsiveTable {...props} />;

export default {
  component: ResponsiveTable,
  args: {
    ariaLabel: "Table title",
    minimumColumnWidth: 250,
    data: sampleData,
    columns,
    hasActions: true,
    hasCustomActionTestId: false,
    hasCustomOuiaIds: false,
    isRowClickable: true,
    isSortable: false,
    sortAllColumns: true,
    selectedRow: 3,
    expectedLength: 3,
  },
  argTypes: {
    hasActions: { control: "boolean" },
    isRowClickable: { control: "boolean" },
    isSortable: { control: "boolean" },
    sortAllColumns: { control: "boolean" },
  },
} as ComponentMeta<typeof ResponsiveTableSampleType>;

const Template: ComponentStory<typeof ResponsiveTableSampleType> = (args) => {
  const [isColumnSortable] = useSortableSearchParams(
    args.sortAllColumns === true
      ? args.columns
      : [args.columns[0], args.columns[3]],
    {
      name: "Name",
      cloudProvider: "Cloud Provider",
      owner: "Owner",
      region: "Region",
      status: "Status",
      timeCreated: "Creation Time",
    }
  );
  return (
    <ResponsiveTable
      {...args}
      renderHeader={({ column, Th, key }) => (
        <Th key={key}>{columnLabels[column]}</Th>
      )}
      renderCell={({ column, row, colIndex, Td, key }) => (
        <Td key={key} dataLabel={columnLabels[column]}>
          {row[colIndex]}
        </Td>
      )}
      renderActions={({ row, ActionsColumn }) =>
        args.hasActions ? (
          <ActionsColumn
            items={
              // @ts-ignore
              defaultActions(row)
            }
          />
        ) : undefined
      }
      isRowSelected={
        args.selectedRow
          ? ({ rowIndex }) => rowIndex === args.selectedRow! - 1
          : undefined
      }
      isRowDeleted={({ row }) => row[5] === "deleting"}
      isColumnSortable={args.isSortable ? isColumnSortable : undefined}
      onRowClick={args.onRowClick || eventsFromNames["onRowClick"]}
      setActionCellOuiaId={
        args.hasCustomActionTestId
          ? ({ rowIndex }) => `my-action-row-${rowIndex}`
          : undefined
      }
      setRowOuiaId={
        args.hasCustomOuiaIds
          ? ({ rowIndex }) => `table-row-${rowIndex}`
          : undefined
      }
      tableOuiaId={args.hasCustomOuiaIds ? "table-ouia-id" : undefined}
    >
      <EmptyState variant={EmptyStateVariant.large}>
        <EmptyStateIcon icon={InfoIcon} />
        <Title headingLevel="h4" size="lg">
          Empty state to show when the data is filtered but has no results
        </Title>
        <EmptyStateBody>
          The <code>children</code> property will be used when no data is
          available as the empty state.
        </EmptyStateBody>
      </EmptyState>
    </ResponsiveTable>
  );
};

export const Example = Template.bind({});
Example.args = {};

export const WithoutActions = Template.bind({});
WithoutActions.args = {
  hasActions: false,
};

export const NonClickableRows = Template.bind({});
NonClickableRows.args = {
  isRowClickable: false,
};

export const NoSelectedRow = Template.bind({});
NoSelectedRow.args = {
  selectedRow: undefined,
};

export const UndefinedDataShowsSkeleton = Template.bind({});
UndefinedDataShowsSkeleton.args = {
  data: undefined,
};

export const NoResults = Template.bind({});
NoResults.args = {
  data: [],
};

export const CustomActionTestId = Template.bind({});
CustomActionTestId.args = {
  hasCustomActionTestId: true,
};

export const CustomOuiaIds = Template.bind({});
CustomOuiaIds.args = {
  hasCustomOuiaIds: true,
};

export const Sortable = Template.bind({});
Sortable.args = {
  isSortable: true,
};

export const PartiallySortable = Template.bind({});
PartiallySortable.args = {
  isSortable: true,
  sortAllColumns: false,
};
