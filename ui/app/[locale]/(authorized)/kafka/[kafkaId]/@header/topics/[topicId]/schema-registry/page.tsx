import {
  TopicHeader,
  TopicHeaderProps,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/topics/[topicId]/TopicHeader";

export default async function TopicHeaderNoRefresh(
  props: Omit<TopicHeaderProps, "showRefresh">,
) {
  const params = await props.params;

  return (
    <TopicHeader 
      {...props} 
      params={params}
      showRefresh={false} 
    />
  );
}
