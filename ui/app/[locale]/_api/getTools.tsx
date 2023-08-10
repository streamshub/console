import {
  ClusterIcon,
  CogIcon,
  DataProcessorIcon,
  DataSinkIcon,
  HatWizardIcon,
  ListIcon,
  TachometerAltIcon,
} from "@/libs/patternfly/react-icons";
import { ReactElement } from "react";

export type Tool = {
  id: string;
  url: string;
  icon: ReactElement;
  title: string;
  description: string;
};

export async function getTools(): Promise<Tool[]> {
  return [
    {
      url: "/principals",
      id: "principals",
      icon: <ClusterIcon />,
      title: "principals.title" as const,
      description: "principals.description" as const,
    },
    {
      url: "/kafka-configuration",
      id: "kafka-configuration",
      icon: <CogIcon />,
      title: "kafka-configuration.title" as const,
      description: "kafka-configuration.description" as const,
    },
    {
      url: "/kafka-insights",
      id: "kafka-insights",
      icon: <TachometerAltIcon />,
      title: "kafka-insights.title" as const,
      description: "kafka-insights.description" as const,
    },
    {
      url: "/topic-manager",
      id: "topic-manager",
      icon: <ListIcon />,
      title: "topic-manager.title" as const,
      description: "topic-manager.description" as const,
    },
    {
      url: "/topic-creator",
      id: "topic-creator",
      icon: <HatWizardIcon />,
      title: "topic-creator.title" as const,
      description: "topic-creator.description" as const,
    },
    {
      url: "/message-browser",
      id: "message-browser",
      icon: <DataProcessorIcon />,
      title: "message-browser.title" as const,
      description: "message-browser.description" as const,
    },
    {
      url: "/message-producer",
      id: "message-producer",
      icon: <DataSinkIcon />,
      title: "message-producer.title" as const,
      description: "message-producer.description" as const,
    },
  ];
}
