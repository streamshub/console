import { EmptyState, EmptyStateBody, Title } from "@patternfly/react-core";
import { InfoIcon } from "@patternfly/react-icons";
import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import type { ResponsiveTableProps } from "./ResponsiveTable";
import { ResponsiveTable } from "./ResponsiveTable";
import {
  columns,
  defaultActions,
  sampleData,
  SampleDataType,
} from "./storybookHelpers";
import { Th } from "@patternfly/react-table";

type ResponsiveTableSampleTypeProps = ResponsiveTableProps<
  SampleDataType,
  (typeof columns)[number]
> & {
  hasActions?: boolean;
  hasCustomActionTestId?: boolean;
  hasCustomOuiaIds?: boolean;
  isRowClickable?: boolean;
  selectedRow?: number;
};
const ResponsiveTableSampleType = (props: ResponsiveTableSampleTypeProps) => (
  <ResponsiveTable {...props} />
);

export default {
  component: ResponsiveTable,
  args: {
    ariaLabel: "Table title",
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
    onRowClick: fn(),
  },
  argTypes: {
    hasActions: { control: "boolean" },
    isRowClickable: { control: "boolean" },
    isSortable: { control: "boolean" },
    sortAllColumns: { control: "boolean" },
  },
  render: (args) => {
    const columnLabels = {
      name: "Name",
      cloudProvider: "Cloud Provider",
      owner: "Owner",
      region: "Region",
      status: "Status",
      timeCreated: "Creation Time",
    };
    return (
      <ResponsiveTable
        {...args}
        renderHeader={({ column, key }) => (
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
        onRowClick={args.isRowClickable ? args.onRowClick : undefined}
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
        <EmptyState
          titleText={
            "Empty state to show when the data is filtered but has no results"
          }
          headingLevel="h4"
          icon={InfoIcon}
          variant={"lg"}
        >
          <EmptyStateBody>
            The <code>children</code> property will be used when no data is
            available as the empty state.
          </EmptyStateBody>
        </EmptyState>
      </ResponsiveTable>
    );
  },
} as Meta<typeof ResponsiveTableSampleType>;

type Story = StoryObj<typeof ResponsiveTableSampleType>;

export const Example: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await canvas.findByText(sampleData[0][0]);

    sampleData
      .flatMap((row) => row.slice(0, -1))
      .forEach((cell) => {
        expect(canvas.getByText(cell)).toBeInTheDocument();
      });
    await expect(canvas.queryAllByTestId("row-selected")).toHaveLength(1);
    await expect(canvas.queryAllByTestId("row-deleted")).toHaveLength(1);
    const actions = canvas.queryAllByLabelText("Actions");
    await expect(canvas.queryAllByLabelText("Kebab toggle")).toHaveLength(
      sampleData.length - 1 /* deleted lines don't have actions */,
    );

    await userEvent.click(actions[0]);
    await expect(args.onRowClick).not.toHaveBeenCalled();

    const firstRow = canvas.getByText(sampleData[0][0]).parentElement!;
    await userEvent.click(firstRow);
    await expect(args.onRowClick).toHaveBeenNthCalledWith(1, {
      row: sampleData[0],
      rowIndex: 0,
    });
  },
};

export const WithoutActions: Story = {
  args: {
    hasActions: false,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await expect(await canvas.queryAllByLabelText("Actions")).toHaveLength(0);
  },
};

export const NonClickableRows: Story = {
  args: {
    isRowClickable: false,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    const firstCell = await canvas.findByText(sampleData[0][0]);
    const firstRow = firstCell.parentElement!;
    await userEvent.click(firstRow);
    await expect(args.onRowClick).not.toHaveBeenCalled();
  },
};

export const NoSelectedRow: Story = {
  args: {
    selectedRow: undefined,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await expect(canvas.queryAllByTestId("row-selected")).toHaveLength(0);
  },
};

export const UndefinedDataShowsSkeleton: Story = {
  args: {
    data: undefined,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await expect(await canvas.findAllByText("Loading data")).toHaveLength(3);
  },
};

export const NoResults: Story = {
  args: {
    data: [],
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);

    await expect(
      await canvas.findByText(
        "Empty state to show when the data is filtered but has no results",
      ),
    ).toBeInTheDocument();
  },
};

export const CustomActionTestId: Story = {
  args: {
    hasCustomActionTestId: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const cell = await canvas.findByTestId("my-action-row-0");
    expect(cell).toBeInTheDocument();
  },
};

export const CustomOuiaIds: Story = {
  args: {
    hasCustomOuiaIds: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);

    const table = await canvas.findByRole("grid");
    expect(table).toHaveAttribute("data-ouia-component-id", "table-ouia-id");
    const firstRow = canvasElement.querySelector(
      "[data-ouia-component-id='table-row-0']",
    );
    expect(firstRow).toBeInTheDocument();
  },
};
