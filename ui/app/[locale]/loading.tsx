import { Loading } from "@/components/Loading";
import { PageSection } from "@/libs/patternfly/react-core";

export default function AppLoading() {
  return (
    <PageSection isFilled={true}>
      <Loading />
    </PageSection>
  );
}
