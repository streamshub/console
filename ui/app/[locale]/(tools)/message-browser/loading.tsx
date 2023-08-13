import { Bullseye, Spinner } from "@/libs/patternfly/react-core";

export default function Loading() {
  return (
    <Bullseye>
      <Spinner />
    </Bullseye>
  );
}
