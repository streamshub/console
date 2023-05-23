import type { FunctionComponent } from "react";
import type { BullseyeProps, SpinnerProps } from "@patternfly/react-core";
import { Bullseye, Spinner } from "@patternfly/react-core";

export type LoadingProps = {
  bullseyeProps?: Omit<BullseyeProps, "children">;
  spinnerProps?: SpinnerProps;
};

export const Loading: FunctionComponent<LoadingProps> = ({
  bullseyeProps,
  spinnerProps,
}: LoadingProps) => {
  return (
    <Bullseye {...bullseyeProps}>
      <Spinner {...spinnerProps} />
    </Bullseye>
  );
};
