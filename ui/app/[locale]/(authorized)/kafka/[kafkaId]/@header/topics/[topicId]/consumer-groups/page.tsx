import {
  TopicHeader,
  TopicHeaderProps,
} from "@/app/[locale]/(authorized)/kafka/[kafkaId]/@header/topics/[topicId]/TopicHeader";

export default function TopicHeaderNoRefresh(
  props: Omit<TopicHeaderProps, "showRefresh">,
) {
  return (
    <TopicHeader /* @next-codemod-error 'props' is used with spread syntax (...). Any asynchronous properties of 'props' must be awaited when accessed. */
    {...props} showRefresh={false} />
  );
}
