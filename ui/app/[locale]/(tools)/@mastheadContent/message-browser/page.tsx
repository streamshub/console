import { getTopics } from "@/api/getTopics";
import { TopicSelector } from "@/components/topicSelector";
import { ToolbarItem } from "@/libs/patternfly/react-core";
import { getSession } from "@/utils/session";
import { useTranslations } from "next-intl";

export default async function Toolbar() {
  const session = await getSession();

  const { topic } = session || {};

  const topics = await getTopics();
  const selectedTopic = topics.find(({ name }) => topic == name);

  return <ToolbarContent selectedTopic={selectedTopic} topics={topics} />;
}

function ToolbarContent({
  selectedTopic,
  topics,
}: {
  selectedTopic: Topic | undefined;
  topics: Topic[];
}) {
  const t = useTranslations("message-browser");
  return (
    <>
      <ToolbarItem>{t("title")}</ToolbarItem>
      <ToolbarItem>
        <TopicSelector selected={selectedTopic} topics={topics} />
      </ToolbarItem>
    </>
  );
}
