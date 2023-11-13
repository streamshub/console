import {
  Divider,
  PageSection,
  Split,
  SplitItem,
  Title,
} from "@/libs/patternfly/react-core";
import { ReactNode } from "react";

export function AppHeader({
  title,
  actions,
  navigation,
}: {
  title: ReactNode;
  actions?: ReactNode[];
  navigation?: ReactNode;
}) {
  return (
    <>
      <PageSection
        variant={"light"}
        padding={{ default: navigation ? "noPadding" : "padding" }}
        className={navigation ? "pf-v5-u-px-lg pf-v5-u-pt-sm" : undefined}
        hasShadowBottom={!navigation}
      >
        <Split hasGutter={true}>
          <SplitItem isFilled={true}>
            <Title headingLevel={"h1"}>{title}</Title>
          </SplitItem>
          {actions &&
            actions.map((a, idx) => <SplitItem key={idx}>{a}</SplitItem>)}
        </Split>
      </PageSection>
      {navigation}
      {navigation && <Divider />}
    </>
  );
}
