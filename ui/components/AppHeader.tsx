import { Divider, PageSection, Title } from "@/libs/patternfly/react-core";
import { ReactNode } from "react";

export function AppHeader({
  title,
  navigation,
}: {
  title: string;
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
        <Title headingLevel={"h1"}>{title}</Title>
      </PageSection>
      {navigation}
      {navigation && <Divider />}
    </>
  );
}
