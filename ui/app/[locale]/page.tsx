import { getTools } from "@/api/getTools";
import { AppMasthead } from "@/components/appMasthead";
import { Search } from "@/components/search";
import { ToolCard } from "@/components/toolCard";
import { Gallery, Page, PageSection } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export default async function Home() {
  const tools = await getTools();
  return <HomeContent tools={tools} />;
}

function HomeContent({ tools }: { tools: Tool[] }) {
  const t = useTranslations();

  return (
    <Page
      header={
        <AppMasthead>
          <Search />
        </AppMasthead>
      }
    >
      <PageSection isFilled>
        <Gallery hasGutter aria-label={t("common.title")}>
          {tools.map((tool) => (
            <ToolCard key={tool.id} {...tool} />
          ))}
        </Gallery>
      </PageSection>
    </Page>
  );
}
