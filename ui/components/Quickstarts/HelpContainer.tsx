"use client";
import dynamic from "next/dynamic";
import React, { PropsWithChildren, useContext } from "react";
import { helpTopics } from "./help-topics";

let HelpTopicContext: any;
try {
  HelpTopicContext = require("@patternfly/quickstarts").HelpTopicContext;
} catch {}

const DynamicHelpTopicContainer = dynamic(
  () => {
    return import("@/libs/patternfly/quickstarts").then(
      (mod) => mod.HelpTopicContainer,
    );
  },
  { ssr: false },
);

export function HelpContainer({ children }: PropsWithChildren) {
  return (
    <DynamicHelpTopicContainer helpTopics={helpTopics}>
      {children}
    </DynamicHelpTopicContainer>
  );
}

export function useHelp() {
  // @ts-ignore
  const { setActiveHelpTopicByName } = useContext(HelpTopicContext);
  return function openHelp(content: "connect-to-cluster") {
    setActiveHelpTopicByName && setActiveHelpTopicByName(content);
  };
}
