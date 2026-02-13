import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
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

type TableViewSampleTypeProps = TableViewProps<
  SampleDataType,
  (typeof columns)[number]
> & {
  selectedRow?: number;
};
const TableViewSampleType = (props: TableViewSampleTypeProps) => (
  <TableView {...props} />
);

export default {
  component: TableViewSampleType,
  args: {
    ariaLabel: "Table title",
    selectedRow: 3,
    expectedLength: 3,
    page: 1,
    perPage: 20,
    itemCount: 397,
    filters: sampleToolbarFilters,
    isFiltered: false,
    emptyStateNoData: SampleEmptyStateNoData,
    emptyStateNoResults: SampleEmptyStateNoResults,
    onPageChange: fn(),
    onClearAllFilters: fn(),
    onRowClick: fn(),
  },
  argTypes: {
    data: {
      table: { disable: true },
      control: {
        type: null,
      },
    },
  },
  render: (args) => {
    const { itemCount, page, perPage = DEFAULT_PERPAGE, data } = args;
    const slicedData = useMemo(() => {
      if (data) {
        if (data.length > 0) {
          return new Array(
            Math.min(perPage, (itemCount || 0) - (page - 1) * perPage),
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
      />
    );
  },
} as Meta<typeof TableViewSampleType>;

type Story = StoryObj<typeof TableViewSampleType>;

export const Example: Story = {
  args: {
    data: sampleData,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByLabelText("Table title")).toBeInTheDocument();
    await expect(canvas.queryAllByLabelText("Pagination")).toHaveLength(2);

    await userEvent.click(canvas.getAllByLabelText("Go to next page")[0]);
    await expect(args.onPageChange).toHaveBeenNthCalledWith(1, 2, 20);

    await expect(
      canvas.queryAllByPlaceholderText("Filter by sample toolbar"),
    ).toHaveLength(2);
  },
};

export const FirstLoadShowsSpinner: Story = {
  args: {
    data: null,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByRole("progressbar")).toBeInTheDocument();
    await expect(
      canvas.queryByLabelText("Table title"),
    ).not.toBeInTheDocument();
  },
};

export const NoInitialDataShowsRightEmptyState: Story = {
  args: {
    data: [],
    isFiltered: false,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(
      canvas.getByText(
        "Empty state to show when the initial loading returns no data",
      ),
    ).toBeInTheDocument();
  },
};

export const LoadingDataAfterFilteringShowsASkeletonAndNoPagination: Story = {
  args: {
    data: undefined,
    itemCount: undefined,
    isFiltered: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(await canvas.findAllByText("Loading data")).toHaveLength(3);
    await expect(canvas.queryByLabelText("Pagination")).not.toBeInTheDocument();
  },
};

export const NoResultsForFilterShowsRightEmptyState: Story = {
  args: {
    data: [],
    isFiltered: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(
      canvas.getByText(
        "Empty state to show when the data is filtered but has no results",
      ),
    ).toBeInTheDocument();
  },
};

export const SinglePageShowsPaginationControlWithDefaultPerPage: Story = {
  args: {
    data: sampleData,
    itemCount: sampleData.length,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getAllByLabelText("Pagination").length).toBe(2);
  },
};

export const SinglePageShowsPaginationControlWithCustomPerPage: Story = {
  args: {
    data: sampleData,
    itemCount: sampleData.length,
    perPage: 10,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getAllByLabelText("Pagination").length).toBe(2);
  },
};

export const LastPage: Story = {
  args: {
    data: [...sampleData, ...sampleData, ...sampleData],
    itemCount: sampleData.length * 3,
    perPage: 5,
    page: 3,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getAllByLabelText("Go to next page")[0], {
      pointerEventsCheck: 0,
    });
    await expect(args.onPageChange).not.toBeCalled();
  },
};
