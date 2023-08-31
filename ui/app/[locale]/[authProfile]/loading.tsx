import { Loading } from "@/components/loading";
import { PageSection } from "@/libs/patternfly/react-core";

export default function Content() {
  return (
    <PageSection isFilled variant={"light"}>
      <Loading />
    </PageSection>
  );
}
