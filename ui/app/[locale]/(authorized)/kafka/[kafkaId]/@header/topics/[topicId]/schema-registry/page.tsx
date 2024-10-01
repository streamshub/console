import {
  TopicHeader,
  TopicHeaderProps,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/topics/[topicId]/TopicHeader";

export default function TopicHeaderNoRefresh(
  props: Omit<TopicHeaderProps, "showRefresh">,
) {
  return <TopicHeader {...props} showRefresh={false} />;
}
