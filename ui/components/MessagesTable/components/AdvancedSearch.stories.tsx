import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { AdvancedSearch } from "./AdvancedSearch";

const meta: Meta<typeof AdvancedSearch> = {
  component: AdvancedSearch,
  args: {
    onSearch: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof AdvancedSearch>;

export const Default: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    const searchInput = canvas.getByRole("textbox");
    await userEvent.type(searchInput, " error{enter}");
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        query: { value: "error", where: "everywhere" },
      }),
    );
  },
};

export const SearchWithPartition: Story = {
  args: {
    filterPartition: 3,
    filterQuery: "error",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getAllByRole("button", { name: /search/i })[1],
    );
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        partition: 3,
        query: {
          value: "error",
          where: "everywhere",
        },
      }),
    );
  },
};

export const SearchWithWhere: Story = {
  args: {
    filterWhere: "headers",
    filterQuery: "User-Agent:Mozilla",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getAllByRole("button", { name: /search/i })[1],
    );
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        query: {
          value: "User-Agent:Mozilla",
          where: "headers",
        },
      }),
    );
  },
};

export const SearchFromTimestamp: Story = {
  args: {
    filterTimestamp: "2024-03-06T09:13:18.662Z",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getAllByRole("button", { name: /search/i })[1],
    );
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        from: {
          type: "timestamp",
          value: "2024-03-06T09:13:18.662Z",
        },
      }),
    );
  },
};

export const SearchUntilLimit: Story = {
  args: {
    filterLimit: 50,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getAllByRole("button", { name: /search/i })[1],
    );
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        limit: 50,
      }),
    );
  },
};

export const SearchContinuously: Story = {
  args: {
    filterLimit: "continuously",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(
      canvas.getAllByRole("button", { name: /search/i })[1],
    );
    await expect(args.onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        limit: "continuously",
      }),
    );
  },
};
