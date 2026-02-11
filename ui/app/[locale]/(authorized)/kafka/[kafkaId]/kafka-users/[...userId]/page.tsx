import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { KafkaUserDetails } from "./KafkaUserDetails";
import { getKafkaUser } from "@/api/kafkaUsers/action";

export async function generateMetadata(props: {
  params: Promise<{ kafkaId: string; userId: string }>;
}) {
  const params = await props.params;
  const t = await getTranslations();

  return {
    title: `${t("kafkausers.kafka_user")} ${params.userId} | ${t("common.title")}`,
  };
}

export default async function ConnectClusterPage({
  params: paramsPromise,
}: {
  params: Promise<{ kafkaId: string; userId: string }>;
}) {
  const params = await paramsPromise;
  return (
    <PageSection>
      <Suspense fallback={<KafkaUserDetails kafkaUser={undefined} />}>
        <ConnectedKafkaUserDetails
          userId={params.userId}
          kafkaId={params.kafkaId}
        />
      </Suspense>
    </PageSection>
  );
}

async function ConnectedKafkaUserDetails({
  userId,
  kafkaId,
}: {
  userId: string;
  kafkaId: string;
}) {
  const response = await getKafkaUser(kafkaId, userId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const kafkaUser = response.payload!;

  return <KafkaUserDetails kafkaUser={kafkaUser} />;
}
