import {
  TopicHeader,
  TopicHeaderProps,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/topics/[topicId]/TopicHeader";

export default async function TopicHeaderNoRefresh(
  props: Omit<TopicHeaderProps, "showRefresh">,
) {
  const params = await props.params;

  const { params: _unused, ...rest } = props;

  return (
    <TopicHeader 
      {...rest} 
      params={params}
      showRefresh={false} 
    />
  );
}
