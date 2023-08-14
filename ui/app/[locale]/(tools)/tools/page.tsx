import { getTools } from "@/api/getTools";
import { ToolCard } from "@/components/toolCard";
import {
  Gallery,
  PageSection,
  Text,
  TextContent,
  Title,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default async function Home() {
  const tools = await getTools();
  return <HomeContent tools={tools} />;
}

function HomeContent({ tools }: { tools: Tool[] }) {
  const t = useTranslations();

  return (
    <>
      <PageSection variant={"light"}>
        <TextContent>
          <Title headingLevel={"h1"}>
            Select a tool to access your Cluster
          </Title>
          <Text>
            Some of the tools might not be fully available depending on the
            access level granted to the selected Principal.
          </Text>
        </TextContent>
      </PageSection>
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {tools.map((tool) => (
            <ToolCard key={tool.id} {...tool} />
          ))}
        </Gallery>
      </PageSection>
    </>
  );
}
