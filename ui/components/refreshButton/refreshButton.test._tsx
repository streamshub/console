import { userEvent } from "@storybook/testing-library";
import { composeStories } from "@storybook/testing-react";
import { render, waitForI18n } from "../../test-utils";
import * as stories from "./refreshButton.stories";

const { Default, Refreshing, Disabled } = composeStories(stories);

describe("RefreshButton", () => {
  it("Default", async () => {
    const clickSpy = jest.fn();
    const comp = render(<Default onClick={clickSpy} />);
    await waitForI18n(comp);
    userEvent.click(comp.getByLabelText("Refresh"));
    expect(clickSpy).toBeCalledTimes(1);
  });

  it("Refreshing", async () => {
    const comp = render(<Refreshing />);
    await waitForI18n(comp);
    expect(comp.getByRole("progressbar")).toBeTruthy();
  });

  it("Disabled", async () => {
    const clickSpy = jest.fn();
    const comp = render(<Disabled onClick={clickSpy} />);
    await waitForI18n(comp);
    expect(comp.getByLabelText("Refresh")).toBeDisabled();
    userEvent.click(comp.getByLabelText("Refresh"));
    expect(clickSpy).toBeCalledTimes(0);
  });
});
