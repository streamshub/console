import { getTranslations } from "next-intl/server";
import { PageSection } from "@/libs/patternfly/react-core";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { KafkaUserDetails } from "./KafkaUserDetails";
import { getKafkaUser } from "@/api/kafkaUsers/action";

export async function generateMetadata(props: {
  params: { kafkaId: string; userId: string };
}) {
  const t = await getTranslations();

  return {
    title: `${t("kafkausers.kafka_user")} ${props.params.userId} | ${t("common.title")}`,
  };
}

export default function ConnectClusterPage({
  params,
}: {
  params: { kafkaId: string; userId: string };
}) {
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
