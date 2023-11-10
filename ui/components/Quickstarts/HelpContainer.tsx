"use client";

import { HelpTopicContext } from "@/libs/patternfly/quickstarts";
import { HelpTopicContainer } from "@patternfly/quickstarts";
import { PropsWithChildren, useContext } from "react";
import { helpTopics } from "./help-topics";

export function HelpContainer({ children }: PropsWithChildren) {
  return (
    <HelpTopicContainer helpTopics={helpTopics}>{children}</HelpTopicContainer>
  );
}

export function useHelp() {
  const { setActiveHelpTopicByName } = useContext(HelpTopicContext);
  return function openHelp(content: "connect-to-cluster") {
    setActiveHelpTopicByName && setActiveHelpTopicByName(content);
  };
}
