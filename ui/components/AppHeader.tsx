import {
  Divider,
  Flex,
  FlexItem,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import { ReactNode } from "react";

export function AppHeader({
  title,
  subTitle,
  actions,
  navigation,
}: {
  title: ReactNode;
  subTitle?: ReactNode;
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
        <Flex>
          <Flex direction={{ default: "column" }}>
            <FlexItem>
              <Title headingLevel={"h1"}>{title}</Title>
            </FlexItem>
            {subTitle && <FlexItem>{subTitle}</FlexItem>}
          </Flex>
          {actions && (
            <Flex
              direction={{ default: "column" }}
              align={{ default: "alignRight" }}
              alignSelf={{ default: "alignSelfFlexEnd" }}
            >
              <Flex>
                {actions.map((a, idx) => (
                  <FlexItem key={idx}>{a}</FlexItem>
                ))}
              </Flex>
            </Flex>
          )}
        </Flex>
      </PageSection>
      {navigation}
      {navigation && <Divider />}
    </>
  );
}
