"use client";
import { setTopicAsViewed } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { useParams } from "next/navigation";
import { PropsWithChildren, useEffect } from "react";

export default function TopicTemplate({ children }: PropsWithChildren) {
  const params = useParams<KafkaTopicParams>();

  useEffect(() => {
    void setTopicAsViewed(params.kafkaId, params.topicId);
  }, [params.kafkaId, params.topicId]);
  return children;
}
