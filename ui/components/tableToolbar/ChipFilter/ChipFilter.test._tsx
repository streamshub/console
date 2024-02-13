import { userEvent, within } from "@storybook/testing-library";
import { composeStories } from "@storybook/testing-react";
import { render, waitForI18n } from "../../../test-utils";
import * as stories from "./ChipFilter.stories";

const {
  DesktopMultipleFilters,
  DesktopMultipleFiltersWithChips,
  MobileMultipleFilters,
  MobileMultipleFiltersWithChips,
  DesktopSingleFilter,
  DesktopSingleFilterWithChips,
  MobileSingleFilter,
  MobileSingleFilterWithChips,
} = composeStories(stories);

describe("ChipFilter", () => {
  it("allows switching between filters on large viewports", async () => {
    const comp = render(<DesktopMultipleFilters />);
    await waitForI18n(comp);

    const toolbar = within(comp.getByTestId("large-viewport-toolbar"));

    expect(toolbar.getByPlaceholderText("Filter by name")).toBeInTheDocument();
    expect(toolbar.queryByText("Filter by options")).not.toBeInTheDocument();

    userEvent.click(toolbar.getByText("Name"));
    userEvent.click(toolbar.getByText("Options"));

    expect(
      toolbar.queryByPlaceholderText("Filter by name")
    ).not.toBeInTheDocument();
    expect(toolbar.getByText("Filter by options")).toBeInTheDocument();
  });

  it("shows all chips on large viewports", async () => {
    const comp = render(<DesktopMultipleFiltersWithChips />);
    await waitForI18n(comp);

    expect(comp.getByText("foo")).toBeInTheDocument();
    expect(comp.getByText("Option 2")).toBeInTheDocument();
  });

  it("allows switching between filters on small viewports after opening the filter panel", async () => {
    const comp = render(<MobileMultipleFilters />);
    await waitForI18n(comp);

    // check that the "desktop" filter is being hidden by the PF toolbar
    expect(
      comp.container
        .querySelector(
          "[data-ouia-component-id='chip-filter-selector-large-viewport']"
        )
        ?.closest(".pf-c-toolbar__item.pf-m-search-filter.pf-m-hidden")
    ).toBeInTheDocument();

    // check that the "mobile" filter is in the toggleable group that is shown after clicking the filter button
    expect(
      comp.container
        .querySelector(
          "[data-ouia-component-id='chip-filter-selector-small-viewport']"
        )
        ?.closest(".pf-c-toolbar__group.pf-m-toggle-group")
    ).toBeInTheDocument();

    userEvent.click(comp.getByLabelText("Show Filters"));

    // check that the "mobile" filter is shown in the expanded area of the toolbar
    expect(
      comp.container
        .querySelector(
          "[data-ouia-component-id='chip-filter-selector-small-viewport']"
        )
        ?.closest(".pf-c-toolbar__expandable-content.pf-m-expanded")
    ).toBeInTheDocument();

    const toolbar = within(
      comp.container.querySelector(
        ".pf-c-toolbar__expandable-content.pf-m-expanded"
      ) as HTMLElement
    );

    userEvent.click(toolbar.getByText("Name"));
    userEvent.click(toolbar.getByText("Options"));

    expect(
      toolbar.queryByPlaceholderText("Filter by name")
    ).not.toBeInTheDocument();
    expect(toolbar.getByText("Filter by options")).toBeInTheDocument();
  });

  it("shows all chips on small viewports", async () => {
    const comp = render(<MobileMultipleFiltersWithChips />);
    await waitForI18n(comp);

    userEvent.click(comp.getByLabelText("Show Filters"));

    const toolbar = within(
      comp.container.querySelector(
        ".pf-c-toolbar__expandable-content.pf-m-expanded"
      ) as HTMLElement
    );

    expect(toolbar.queryByText("2 filters applied")).not.toBeInTheDocument();
    expect(toolbar.getByText("foo")).toBeVisible();
    expect(toolbar.getByText("Option 2")).toBeVisible();
  });

  it("shows a single filter with no selection on large viewports", async () => {
    const comp = render(<DesktopSingleFilter />);
    await waitForI18n(comp);

    expect(
      comp.queryByTestId("chip-filter-selector-large-viewport")
    ).not.toBeInTheDocument();
    const toolbar = within(comp.getByTestId("large-viewport-toolbar"));
    expect(toolbar.getByPlaceholderText("Filter by name")).toBeInTheDocument();
  });

  it("shows all chips on large viewports with a single filter", async () => {
    const comp = render(<DesktopSingleFilterWithChips />);
    await waitForI18n(comp);

    expect(comp.getByText("foo")).toBeInTheDocument();
  });

  it("shows a single filter on small viewports after opening the filter panel", async () => {
    const comp = render(<MobileSingleFilter />);
    await waitForI18n(comp);

    expect(
      comp.queryByTestId("chip-filter-selector-small-viewport")
    ).not.toBeInTheDocument();

    userEvent.click(comp.getByLabelText("Show Filters"));

    const toolbar = within(
      comp.container.querySelector(
        ".pf-c-toolbar__expandable-content.pf-m-expanded"
      ) as HTMLElement
    );

    expect(
      toolbar.queryByPlaceholderText("Filter by name")
    ).toBeInTheDocument();
  });

  it("shows all chips on small viewports with a single filter", async () => {
    const comp = render(<MobileSingleFilterWithChips />);
    await waitForI18n(comp);

    userEvent.click(comp.getByLabelText("Show Filters"));

    const toolbar = within(
      comp.container.querySelector(
        ".pf-c-toolbar__expandable-content.pf-m-expanded"
      ) as HTMLElement
    );
    expect(toolbar.getByText("foo")).toBeInTheDocument();
  });
});
