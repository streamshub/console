"use client";
import {
  QuickStartCatalogPage,
  QuickStartContainer,
  useLocalStorage,
} from "@/libs/patternfly/quickstarts";
import { useEffect, useState } from "react";
import {
  explorePipelinesQuickStart,
  exploreServerlessQuickStart,
  monitorSampleAppQuickStart,
} from "./example-quickstarts";

const exampleQuickStarts = [
  explorePipelinesQuickStart,
  exploreServerlessQuickStart,
  monitorSampleAppQuickStart,
];

export default function LearningResources() {
  const [activeQuickStartID, setActiveQuickStartID] = useLocalStorage(
    "quickstartId",
    "",
  );
  const [allQuickStartStates, setAllQuickStartStates] = useLocalStorage(
    "quickstarts",
    {},
  );
  const language = localStorage.getItem("bridge/language") || "en";

  // eslint-disable-next-line no-console
  useEffect(() => console.log(activeQuickStartID), [activeQuickStartID]);
  useEffect(() => {
    // callback on state change
    // eslint-disable-next-line no-console
    console.log(allQuickStartStates);
  }, [allQuickStartStates]);

  const [loading, setLoading] = useState(true);
  const [quickStarts, setQuickStarts] = useState([]);
  useEffect(() => {
    const load = async () => {
      setQuickStarts(exampleQuickStarts);
      setLoading(false);
    };
    setTimeout(() => {
      load();
    }, 500);
  }, [quickStarts]);
  const drawerProps = {
    quickStarts,
    activeQuickStartID,
    allQuickStartStates,
    setActiveQuickStartID,
    setAllQuickStartStates,
    showCardFooters: true,
    language,
    loading,
    alwaysShowTaskReview: true,
    markdown: {
      extensions: [
        // variable substitution
        {
          type: "output",
          filter(html: string) {
            html = html.replace(/\[APPLICATION\]/g, "Mercury");
            html = html.replace(/\[PRODUCT\]/g, "Lightning");

            return html;
          },
        },
      ],
    },
  };

  return (
    <QuickStartContainer {...drawerProps}>
      <QuickStartCatalogPage
        title="Quick starts"
        hint={
          "Learn how to create, import, and run applications with step-by-step instructions and tasks."
        }
      />
    </QuickStartContainer>
  );
}
