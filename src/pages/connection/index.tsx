import {
  Breadcrumb,
  BreadcrumbHeading,
  BreadcrumbItem,
  Card,
  CardBody,
  CardFooter,
  CardTitle,
  Gallery,
  Label,
  PageSection,
  PageSectionVariants,
  SearchInput,
  Text,
  TextContent,
  Title,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { type NextPage } from "next";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useRouter } from "next/router";
import React from "react";
import Layout from "~/components/layout";

const tools = [
  {
    title: "Topic developer",
    description: "Can produce and consume messages from the consumer topic",
    labels: ["Development", "Consume", "Produce"],
  },
  {
    title: "Development cluster admin",
    description: "Full access to the development cluster",
    labels: ["Development", "Administrator"],
  },
  {
    title: "Production cluster admin",
    description: "Full access to the development cluster",
    labels: ["Production", "Administrator"],
  },
  {
    title: " Production topic consumer",
    description: "Full access to the cluster",
    labels: ["Consume", "Production"],
  },
];

const Connection: NextPage = () => {
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
    <Layout
      breadcrumb={
        <Breadcrumb>
          <BreadcrumbItem
            isActive
            render={(props) => (
              <Breadcrumb>
                <BreadcrumbItem
                  render={(props) => (
                    <Link {...props} href={"/"}>
                      {t("common.title")}
                    </Link>
                  )}
                />
                <BreadcrumbHeading>{t("connection.title")}</BreadcrumbHeading>
              </Breadcrumb>
            )}
          />
        </Breadcrumb>
      }
    >
      <PageSection variant={PageSectionVariants.light}>
        <TextContent>
          <Title headingLevel={"h1"} size="4xl">
            {t("connection.title")}
          </Title>
          <Text>{t("connection.description")}</Text>
        </TextContent>
        <Toolbar id="toolbar-group-types" clearAllFilters={() => {}}>
          <ToolbarContent>{toolbarItems}</ToolbarContent>
        </Toolbar>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {tools.map(({ title, description, labels }, idx) => (
            <Card isClickable={true} key={idx}>
              <CardTitle>{title}</CardTitle>
              <CardBody>{description}</CardBody>
              <CardFooter>
                {labels.map((l, idx) => (
                  <Label key={idx}>{l}</Label>
                ))}
              </CardFooter>
            </Card>
          ))}
        </Gallery>
      </PageSection>
    </Layout>
  );
};

export { getStaticProps } from "~/utils/getStaticProps";

export default Connection;
