import { userEvent } from "@storybook/testing-library";
import { composeStories } from "@storybook/testing-react";
import { render, waitForI18n } from "../../test-utils";
import * as stories from "./TableView.stories";

const {
  Example,
  FirstLoadShowsSpinner,
  NoInitialDataShowsRightEmptyState,
  LoadingDataAfterFilteringShowsASkeletonAndNoPagination,
  NoResultsForFilterShowsRightEmptyState,
  SinglePageShowsNoPaginationControl,
} = composeStories(stories);

describe("TableView", () => {
  it("renders the Example", async () => {
    const clickSpy = jest.fn();
    const comp = render(<Example onPageChange={clickSpy} />);
    await waitForI18n(comp);

    expect(comp.getByLabelText("Table title")).toBeInTheDocument();
    expect(comp.queryAllByLabelText("Pagination")).toHaveLength(2);

    userEvent.click(comp.getAllByLabelText("Go to next page")[0]);
    expect(clickSpy).toHaveBeenNthCalledWith(1, 2, 10);

    expect(
      comp.queryAllByPlaceholderText("Filter by sample toolbar")
    ).toHaveLength(2);
  });

  it("renders only a spinner the first time data is loaded", () => {
    const comp = render(<FirstLoadShowsSpinner />);

    expect(comp.getByRole("progressbar")).toBeInTheDocument();
    expect(comp.queryByLabelText("Table title")).not.toBeInTheDocument();
  });

  it("renders the empty state", () => {
    const comp = render(<NoInitialDataShowsRightEmptyState />);
    expect(
      comp.getByText(
        "Empty state to show when the initial loading returns no data"
      )
    ).toBeInTheDocument();
  });

  it("shows a skeleton loader with undefined data but with a known item count", async () => {
    const comp = render(
      <LoadingDataAfterFilteringShowsASkeletonAndNoPagination />
    );
    expect(await comp.findAllByText("Loading content")).toHaveLength(3);
    expect(comp.queryByLabelText("Pagination")).not.toBeInTheDocument();
  });

  it("renders the empty state", () => {
    const comp = render(<NoResultsForFilterShowsRightEmptyState />);
    expect(
      comp.getByText(
        "Empty state to show when the data is filtered but has no results"
      )
    ).toBeInTheDocument();
  });

  it("hides the pagination for a single page of data", () => {
    const comp = render(<SinglePageShowsNoPaginationControl />);
    expect(comp.queryByLabelText("Pagination")).not.toBeInTheDocument();
  });
});
