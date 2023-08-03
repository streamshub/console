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
  Text,
  TextContent,
  Toolbar,
  ToolbarContent,
  ToolbarFilter,
  ToolbarItem,
} from "@patternfly/react-core";
import { ClusterIcon, DataProcessorIcon } from "@patternfly/react-icons";
import { type GetStaticProps, type NextPage } from "next";
import { useTranslations } from "next-intl";
import { useRouter } from "next/router";
import React from "react";
import Layout from "~/components/layout";

const tools = [
  {
    url: "/data-source",
    id: "data-source",
    icon: <ClusterIcon />,
    title: "data-source.title" as const,
    description: "data-source.description" as const,
  },
  {
    url: "/message-browser",
    id: "message-browser",
    icon: <DataProcessorIcon />,
    title: "message-browser.title" as const,
    description: "message-browser.description" as const,
  },
];

const Home: NextPage = () => {
  const t = useTranslations();
  const router = useRouter();

  const toolbarItems = (
    <>
      <ToolbarItem>
        <ToolbarFilter categoryName="Products" chips={[]} deleteChip={() => {}}>
          <SearchInput
            placeholder="Find by name"
            value={""}
            onChange={(_event, value) => {}}
            onClear={() => {}}
          />
        </ToolbarFilter>
      </ToolbarItem>
    </>
  );

  return (
    <Layout>
      <PageSection variant={PageSectionVariants.light}>
        <TextContent>
          <Text component="h1">{t("homepage.title")}</Text>
          <Text component="p">{t("homepage.description")}</Text>
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

export const getStaticProps: GetStaticProps = async function (context) {
  return {
    props: {
      // You can get the messages from anywhere you like. The recommended
      // pattern is to put them in JSON files separated by locale and read
      // the desired one based on the `locale` received from Next.js.
      messages: (await import(`../../messages/${context.locale || "en"}.json`))
        .default as IntlMessages,
    },
  };
};

export default Home;
