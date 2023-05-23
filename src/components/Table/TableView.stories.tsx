import { actions } from "@storybook/addon-actions";
import type { ComponentMeta, ComponentStory } from "@storybook/react";
import type { VoidFunctionComponent } from "react";
import { useMemo } from "react";
import type { SampleDataType } from "./storybookHelpers";
import {
  columnLabels,
  columns,
  defaultActions,
  sampleData,
  SampleEmptyStateNoData,
  SampleEmptyStateNoResults,
  sampleToolbarFilters,
} from "./storybookHelpers";
import type { TableViewProps } from "./TableView";
import { DEFAULT_PERPAGE, TableView } from "./TableView";

const eventsFromNames = actions("onRowClick");

const TableViewSampleType: VoidFunctionComponent<
  TableViewProps<SampleDataType, typeof columns[number]> & {
    selectedRow?: number;
  }
> = (props) => <TableView {...props} />;

export default {
  component: TableView,
  args: {
    ariaLabel: "Table title",
    minimumColumnWidth: 250,
    selectedRow: 3,
    expectedLength: 3,
    page: 1,
    perPage: 10,
    itemCount: 397,
    filters: sampleToolbarFilters,
    isFiltered: false,
    emptyStateNoData: SampleEmptyStateNoData,
    emptyStateNoResults: SampleEmptyStateNoResults,
  },
  argTypes: {
    data: {
      table: { disable: true },
      control: {
        type: null,
      },
    },
  },
} as ComponentMeta<typeof TableViewSampleType>;

const Template: ComponentStory<typeof TableViewSampleType> = (args) => {
  const { itemCount, page, perPage = DEFAULT_PERPAGE, data } = args;
  const slicedData = useMemo(() => {
    if (data) {
      if (data.length > 0) {
        return new Array(
          Math.min(perPage, (itemCount || 0) - (page - 1) * perPage)
        )
          .fill(0)
          .map((_, index) => {
            return data[index % sampleData.length];
          });
      }
      return [];
    }
    return data;
  }, [data, itemCount, page, perPage]);
  return (
    <TableView
      {...args}
      data={slicedData}
      columns={columns}
      renderHeader={({ column, Th, key }) => (
        <Th key={key}>{columnLabels[column]}</Th>
      )}
      renderCell={({ column, row, colIndex, Td, key }) => (
        <Td key={key} dataLabel={columnLabels[column]}>
          {row[colIndex]}
        </Td>
      )}
      renderActions={({ row, ActionsColumn }) => (
        <ActionsColumn items={defaultActions(row)} />
      )}
      isRowSelected={
        args.selectedRow
          ? ({ rowIndex }) => rowIndex === args.selectedRow! - 1
          : undefined
      }
      isRowDeleted={({ row }) => row[5] === "deleting"}
      {...eventsFromNames}
    />
  );
};

export const Example = Template.bind({});
Example.args = {
  data: sampleData,
};

export const FirstLoadShowsSpinner = Template.bind({});
FirstLoadShowsSpinner.args = {
  data: null,
};

export const NoInitialDataShowsRightEmptyState = Template.bind({});
NoInitialDataShowsRightEmptyState.args = {
  data: [],
  isFiltered: false,
};

export const LoadingDataAfterFilteringShowsASkeletonAndNoPagination =
  Template.bind({});
LoadingDataAfterFilteringShowsASkeletonAndNoPagination.args = {
  data: undefined,
  itemCount: undefined,
  isFiltered: true,
};

export const NoResultsForFilterShowsRightEmptyState = Template.bind({});
NoResultsForFilterShowsRightEmptyState.args = {
  data: [],
  isFiltered: true,
};

export const SinglePageShowsNoPaginationControl = Template.bind({});
SinglePageShowsNoPaginationControl.args = {
  data: sampleData,
  itemCount: sampleData.length,
};

export const LastPage = Template.bind({});
LastPage.args = {
  data: sampleData,
  itemCount: 27,
  page: 3,
};
