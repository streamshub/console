import { getTools, Tool } from "@/app/[locale]/_api/getTools";
import { ToolCard } from "@/app/[locale]/_components/toolCard";
import { Gallery, PageSection } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default async function Home() {
  const tools = await getTools();
  return <HomeContent tools={tools} />;
}

function HomeContent({ tools }: { tools: Tool[] }) {
  const t = useTranslations();

  return (
    <>
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
