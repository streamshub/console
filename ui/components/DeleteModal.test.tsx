import { renderDialog, waitFor } from "../../test-utils";
import { composeStories } from "@storybook/testing-react";
import * as stories from "./DeleteModal.stories";
import { userEvent } from "@storybook/testing-library";

const {
  SynchronousDelete,
  SynchronousDeleteWithError,
  SynchronousDeleteWithConfirmation,
  AsynchronousDelete,
  AsynchronousDeleteWithError,
  AsynchronousDeleteWithConfirmation,
} = composeStories(stories);

describe("DeleteModal", () => {
  it("SynchronousDelete - can delete", async () => {
    const onDelete = jest.fn();
    const onCancel = jest.fn();

    const comp = renderDialog(
      <SynchronousDelete onDelete={onDelete} onCancel={onCancel} />
    );
    expect(await comp.findByText("You are deleting something.")).toBeTruthy();
    userEvent.click(await comp.findByText("Delete"));
    await waitFor(() => {
      expect(
        comp.queryByText("You are deleting something.")
      ).not.toBeInTheDocument();
    });
    expect(onDelete).toBeCalledTimes(1);
    expect(onCancel).not.toBeCalled();
  });

  it("SynchronousDelete - can cancel", async () => {
    const onDelete = jest.fn();
    const onCancel = jest.fn();

    const comp = renderDialog(
      <SynchronousDelete onDelete={onDelete} onCancel={onCancel} />
    );
    expect(await comp.findByText("You are deleting something.")).toBeTruthy();
    userEvent.click(await comp.findByText("Cancel"));
    await waitFor(() => {
      expect(
        comp.queryByText("You are deleting something.")
      ).not.toBeInTheDocument();
    });
    expect(onCancel).toBeCalledTimes(1);
    expect(onDelete).not.toBeCalled();
  });

  it("SynchronousDeleteWithError", async () => {
    const comp = renderDialog(<SynchronousDeleteWithError />);
    expect(await comp.findByText("Danger alert title")).toBeTruthy();
  });

  it("SynchronousDeleteWithConfirmation", async () => {
    const comp = renderDialog(<SynchronousDeleteWithConfirmation />);
    await SynchronousDeleteWithConfirmation.play({
      canvasElement: comp.container,
    });
    await waitFor(() => {
      expect(
        comp.queryByText("You are deleting something.")
      ).not.toBeInTheDocument();
    });
  });

  it("AsynchronousDelete - delete button disabled after clicking", async () => {
    const comp = renderDialog(<AsynchronousDelete />);
    const button = await comp.findByText("Delete");
    userEvent.click(button);
    expect(button).toBeDisabled();
  });

  it("AsynchronousDelete - can retry in case of error", async () => {
    const comp = renderDialog(<AsynchronousDeleteWithError />);
    const button = await comp.findByText("Delete");
    userEvent.click(button);
    expect(button).toBeDisabled();
    expect(await comp.findByText("Danger alert title")).toBeTruthy();
    expect(button).toBeEnabled();
  });

  it("AsynchronousDelete - can delete", async () => {
    const onDelete = jest.fn();
    const onCancel = jest.fn();

    const comp = renderDialog(
      <AsynchronousDeleteWithConfirmation
        onCancel={onCancel}
        onDelete={onDelete}
      />
    );
    await AsynchronousDeleteWithConfirmation.play({
      canvasElement: comp.container,
    });
    await waitFor(() => {
      expect(
        comp.queryByText("You are deleting something.")
      ).not.toBeInTheDocument();
    });
    expect(onDelete).toBeCalledTimes(1);
    expect(onCancel).not.toBeCalled();
  });
});
