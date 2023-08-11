import type { BullseyeProps, SpinnerProps } from "@/libs/patternfly/react-core";
import { Bullseye, Spinner } from "@/libs/patternfly/react-core";

export type LoadingProps = {
  bullseyeProps?: Omit<BullseyeProps, "children">;
  spinnerProps?: SpinnerProps;
};

export function Loading({ bullseyeProps, spinnerProps }: LoadingProps) {
  return (
    <Bullseye {...bullseyeProps}>
      <Spinner {...spinnerProps} />
    </Bullseye>
  );
}
