import { getTranslations } from "next-intl/server";
import { ApiResponse } from "@/api/api";
import { getTopic, updateTopic } from "@/api/topics/actions";
import { KafkaTopicParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/kafkaTopic.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { clientConfig as config } from "@/utils/config";
import { Suspense } from "react";
import { ConfigTable } from "./ConfigTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata() {
  const t = await getTranslations();

  return {
    title: `Topic Configuration | ${t("common.title")}`,
  };
}

export default async function TopicConfiguration(
  props: {
    params: Promise<KafkaTopicParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId,
    topicId
  } = params;

  return (
    <PageSection isFilled={true}>
      <Suspense
        fallback={<ConfigTable topic={undefined} onSaveProperty={undefined} />}
      >
        <ConnectedTopicConfiguration params={{ kafkaId, topicId }} />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedTopicConfiguration({
  params: { kafkaId, topicId },
}: {
  params: KafkaTopicParams;
}) {
  const response = await getTopic(kafkaId, topicId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const topic = response.payload!;

  async function onSaveProperty(name: string, value: string) {
    "use server";
    const isReadOnly = (await config()).readOnly;

    if (isReadOnly) {
      // silently ignore attempt to change a property value in read-only mode
      return Promise.resolve({ payload: undefined } as ApiResponse<undefined>);
    }

    return updateTopic(kafkaId, topicId, undefined, undefined, {
      [name]: {
        value,
      },
    });
  }

  return <ConfigTable topic={topic} onSaveProperty={onSaveProperty} />;
}
