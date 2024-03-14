import { EmptyStateLoading } from "@/components/EmptyStateLoading";
import { PageSection } from "@/libs/patternfly/react-core";

export default function AppLoading() {
  return (
    <PageSection isFilled={true}>
      <EmptyStateLoading />
    </PageSection>
  );
}
