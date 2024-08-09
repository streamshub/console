import { EmptyStateLoading } from "@/components/EmptyStateLoading";
import { PageSection } from "@/libs/patternfly/react-core";

export default function AppLoading() {
  return (
    <div style={{ width: "100vw", height: "100vh" }}>
      <EmptyStateLoading />
    </div>
  );
}
