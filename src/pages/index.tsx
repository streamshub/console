import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Gallery,
  Icon,
  PageSection,
  PageSectionVariants,
  SearchInput,
  TextContent,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import {
  ClusterIcon,
  DataProcessorIcon,
  DataSinkIcon,
  HatWizardIcon,
  ListIcon,
  TachometerAltIcon,
} from "@patternfly/react-icons";
import CogIcon from "@patternfly/react-icons/dist/esm/icons/cog-icon";
import { type NextPage } from "next";
import { useTranslations } from "next-intl";
import { useRouter } from "next/router";
import React from "react";
import Layout from "~/components/layout";

const tools = [
  {
    url: "/connection",
    id: "connection",
    icon: <ClusterIcon />,
    title: "connection.title" as const,
    description: "connection.description" as const,
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

const Home: NextPage = () => {
  const t = useTranslations();
  const router = useRouter();

  const toolbarItems = (
    <>
      <ToolbarItem variant={"search-filter"}>
        <SearchInput
          value={""}
          onChange={(_event, value) => {}}
          onClear={() => {}}
        />
      </ToolbarItem>
    </>
  );

  return (
    <Layout>
      <PageSection variant={PageSectionVariants.light}>
        <TextContent>
          <Title headingLevel={"h1"} size="4xl">
            {t("homepage.title")}
          </Title>
        </TextContent>
        <Toolbar id="toolbar-group-types" clearAllFilters={() => {}}>
          <ToolbarContent>{toolbarItems}</ToolbarContent>
        </Toolbar>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {tools.map(({ id, url, icon, title, description }) => (
            <Card isClickable={true} key={id}>
              <CardHeader
                selectableActions={{
                  onClickAction: () => void router.push(url),
                  selectableActionId: id,
                }}
              >
                <Icon size={"xl"}>{icon}</Icon>
              </CardHeader>
              <CardTitle>{t(title)}</CardTitle>
              <CardBody>{t(description)}</CardBody>
            </Card>
          ))}
        </Gallery>
      </PageSection>
    </Layout>
  );
};

export { getStaticProps } from "~/utils/getStaticProps";

export default Home;
