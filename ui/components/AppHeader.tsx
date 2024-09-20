"use client";
import { DateTime } from "@/components/Format/DateTime";
import { RefreshButton } from "@/components/RefreshButton";
import {
  Divider,
  Flex,
  FlexItem,
  PageSection,
  Title,
} from "@/libs/patternfly/react-core";
import { ReactNode } from "react";
import { useNow } from "use-intl";

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
  const lastRefresh = useNow();
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
          <Flex
            direction={{ default: "column" }}
            align={{ default: "alignRight" }}
          >
            <Flex className={"pf-v5-u-font-size-sm"}>
              Last updated{" "}
              <DateTime
                value={lastRefresh}
                dateStyle={"short"}
                timeStyle={"medium"}
                tz={"local"}
              />
              <RefreshButton lastRefresh={lastRefresh} />
            </Flex>
            {actions && (
              <Flex alignSelf={{ default: "alignSelfFlexEnd" }}>
                {actions.map((a, idx) => (
                  <FlexItem key={idx}>{a}</FlexItem>
                ))}
              </Flex>
            )}
          </Flex>
        </Flex>
      </PageSection>
      {navigation}
      {navigation && <Divider />}
    </>
  );
}
