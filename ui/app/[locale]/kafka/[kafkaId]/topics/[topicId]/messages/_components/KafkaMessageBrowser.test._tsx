import { userEvent } from "@storybook/testing-library";
import { composeStories } from "@storybook/testing-react";
import { waitFor, within } from "@testing-library/react";
import { render, waitForI18n, waitForPopper } from "../../test-utils";
import * as stories from "./KafkaMessageBrowser.stories";

const { Example, InitialLoading, NoData, ApiError, NoMatch } =
  composeStories(stories);

describe("KafkaMessageBrowser", () => {
  it("renders the data, can click on a row to see the details for a message", async () => {
    const getMessages = jest.fn(Example.args!.getMessages);
    const comp = render(<Example getMessages={getMessages} />);
    await waitForI18n(comp);
    expect(getMessages).toBeCalledTimes(1);
    const rows = await comp.findAllByRole("row");
    // 1 row for the header plus 10 rows of data
    expect(rows).toHaveLength(11);

    const row = within(rows[2]);

    userEvent.click(
      row.getAllByText("this-is-a-very-long-key", { exact: false })[0]
    );
    const details = await comp.findByTestId("message-details");
    expect(details).toBeInTheDocument();
    let detailsCont = within(details);
    expect(
      detailsCont.getByText(
        "this-is-a-very-long-key-that-might-cause-some-trouble-figuring-out-column-widths"
      )
    );
    userEvent.click(comp.getByLabelText("Close drawer panel"));
    await waitFor(() => {
      expect(comp.queryByTestId("message-details")).not.toBeInTheDocument();
    });

    // clicking on "Show more" of a Header cell shows the Headers tab in the details drawer
    userEvent.click(row.getAllByText("Show more")[1]);
    detailsCont = within(await comp.findByTestId("message-details"));
    expect(
      detailsCont.getByText('"post-office-box"', {
        exact: false,
      })
    ).toBeInTheDocument();
    expect(
      detailsCont.getByDisplayValue('"post-office-box"', {
        exact: false,
      })
    ).toBeInTheDocument();
    userEvent.click(comp.getByLabelText("Close drawer panel"));

    // clicking on "Show more" of a Value cell shows the Value tab in the details drawer
    userEvent.click(row.getAllByText("Show more")[2]);
    detailsCont = within(await comp.findByTestId("message-details"));
    expect(
      detailsCont.getByText('"123 any st"', {
        exact: false,
      })
    ).toBeInTheDocument();
    expect(
      detailsCont.getByDisplayValue('"123 any st"', {
        exact: false,
      })
    ).toBeInTheDocument();
  });

  it("allows for filtering by offset", async () => {
    const getMessages = jest.fn(Example.args!.getMessages);
    const comp = render(<Example getMessages={getMessages} />);
    await waitForI18n(comp);

    const toolbar = within(await comp.findByTestId("message-browser-toolbar"));
    const filterGroup = await toolbar.findByTestId("filter-group");
    expect(filterGroup).toBeInTheDocument();

    // change partition
    userEvent.type(
      await toolbar.findByLabelText("Specify partition value"),
      "1337"
    );
    userEvent.keyboard("[ArrowDown][Enter]");
    userEvent.click(filterGroup);
    // change offset
    userEvent.click(toolbar.getByText("Offset"));
    userEvent.type(toolbar.getByLabelText("Specify offset"), "1337");

    const search = await toolbar.findByLabelText("Search");
    const refresh = await toolbar.findByLabelText("Refresh");

    // check for the buttons' states
    expect(search).toBeEnabled();
    expect(refresh).toBeDisabled();

    // apply the search
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: 1337,
      offset: 1337,
      timestamp: undefined,
    });

    // verify the correct states of the buttons after the search
    expect(search).toBeDisabled();
    expect(refresh).toBeEnabled();

    // verify that refresh calls the api with the applied filters
    userEvent.click(refresh);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: 1337,
      offset: 1337,
      timestamp: undefined,
    });

    // reset the filter
    await waitFor(() => expect(filterGroup).not.toBeDisabled());
    userEvent.click(filterGroup);
    // change offset
    userEvent.click(toolbar.getByText("Latest messages"));
    expect(search).toBeEnabled();
    expect(refresh).toBeDisabled();
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: 1337,
      offset: undefined,
      timestamp: undefined,
    });
  });

  it("allows for filtering by timestamp", async () => {
    const getMessages = jest.fn(Example.args!.getMessages);
    const comp = render(<Example getMessages={getMessages} />);
    await waitForI18n(comp);

    const toolbar = within(await comp.findByTestId("message-browser-toolbar"));
    const filterGroup = await toolbar.findByTestId("filter-group");
    expect(filterGroup).toBeInTheDocument();

    const search = await toolbar.findByLabelText("Search");
    const refresh = await toolbar.findByLabelText("Refresh");

    // change timestamp
    userEvent.click(filterGroup);
    userEvent.click(toolbar.getByText("Timestamp"));
    userEvent.type(toolbar.getByLabelText("Date picker"), "2022-04-12");
    userEvent.click(toolbar.getByLabelText("Time picker"));
    userEvent.click(await toolbar.findByText("6:00 PM"));
    await waitForPopper();

    // check for the buttons' states
    expect(search).toBeEnabled();
    expect(refresh).toBeDisabled();
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: undefined,
      offset: undefined,
      timestamp: "2022-04-12T18:00:00Z",
    });
  });

  it("allows for filtering by epoch", async () => {
    const getMessages = jest.fn(Example.args!.getMessages);
    const comp = render(<Example getMessages={getMessages} />);
    await waitForI18n(comp);

    const toolbar = within(await comp.findByTestId("message-browser-toolbar"));
    const filterGroup = await toolbar.findByTestId("filter-group");
    expect(filterGroup).toBeInTheDocument();

    const search = await toolbar.findByLabelText("Search");
    const refresh = await toolbar.findByLabelText("Refresh");

    // change epoch
    userEvent.click(filterGroup);
    userEvent.click(toolbar.getByText("Epoch timestamp"));
    userEvent.type(
      toolbar.getByLabelText("Specify epoch timestamp"),
      "1650637783"
    );

    // check for the buttons' states
    expect(search).toBeEnabled();
    expect(refresh).toBeDisabled();
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: undefined,
      offset: undefined,
      timestamp: undefined,
    });

    await waitFor(() => expect(filterGroup).not.toBeDisabled());
    userEvent.clear(toolbar.getByLabelText("Specify epoch timestamp"));
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 10,
      partition: undefined,
      offset: undefined,
      timestamp: undefined,
    });
  });

  it("allows for changing the limit", async () => {
    const getMessages = jest.fn(Example.args!.getMessages);
    const comp = render(<Example getMessages={getMessages} />);
    await waitForI18n(comp);

    const toolbar = within(await comp.findByTestId("message-browser-toolbar"));

    const search = await toolbar.findByLabelText("Search");
    const refresh = await toolbar.findByLabelText("Refresh");

    // change limit
    userEvent.click(toolbar.getByText("10 messages"));
    userEvent.click(toolbar.getByText("50 messages"));

    // check for the buttons' states
    expect(search).toBeEnabled();
    expect(refresh).toBeDisabled();
    userEvent.click(search);
    expect(getMessages).toHaveBeenLastCalledWith({
      limit: 50,
      partition: undefined,
      offset: undefined,
      timestamp: undefined,
    });
  });

  it("renders the right empty state if there are no messages at all", async () => {
    const getMessages = jest.fn(NoData.args!.getMessages);
    const comp = render(<NoData getMessages={getMessages} />);
    await waitForI18n(comp);
    expect(
      await comp.findByText(
        "Data will appear shortly after we receive produced messages."
      )
    ).toBeInTheDocument();
    expect(getMessages).toBeCalledTimes(1);
    userEvent.click(comp.getByText("Check for new data"));
    expect(getMessages).toBeCalledTimes(2);
  });

  it("renders the right empty state if the API fails returning data", async () => {
    const getMessages = jest.fn(ApiError.args!.getMessages);
    const comp = render(<ApiError getMessages={getMessages} />);
    await waitForI18n(comp);
    expect(
      await comp.findByText(
        "Data will appear shortly after we receive produced messages."
      )
    ).toBeInTheDocument();
    expect(getMessages).toBeCalledTimes(1);
    userEvent.click(comp.getByText("Check for new data"));
    expect(getMessages).toBeCalledTimes(2);
  });

  it("renders a single spinner when initially loading the data", async () => {
    const comp = render(<InitialLoading />);
    await waitForI18n(comp);
    expect(comp.getByRole("progressbar")).toBeInTheDocument();
  });

  it("renders the right empty state if after a filtering there is no data", async () => {
    const comp = render(<NoMatch />);
    await waitForI18n(comp);
    await NoMatch.play({ canvasElement: comp.container });
    expect(
      await comp.findByText("Adjust your selection criteria and try again.")
    ).toBeInTheDocument();
    userEvent.click(comp.getByText("Show latest messages"));
    await waitFor(() => expect(comp.queryAllByRole("row")).toHaveLength(11));
  });
});
