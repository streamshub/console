import { AppHeader } from "@/components/AppHeader";
import { getTranslations } from "next-intl/server";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { Suspense } from "react";
import RichText from "@/components/RichText";
import { KafkaUserParams } from "../../../kafka-users/kafkaUser.params";
import { getKafkaUser } from "@/api/kafkaUsers/action";

export default async function Page({
  params: paramsPromise,
}: {
  params: Promise<KafkaUserParams>;
}) {
  const { kafkaId, userId } = await paramsPromise;
  return (
    <Suspense fallback={<Header params={{ kafkaId, userId }} />}>
      <KafkaUserAppHeader params={{ kafkaId, userId }} />
    </Suspense>
  );
}

async function KafkaUserAppHeader({
  params: { kafkaId, userId },
}: {
  params: KafkaUserParams;
}) {
  const response = await getKafkaUser(kafkaId, userId);

  if (response.errors) {
    return <NoDataErrorState errors={response.errors} />;
  }

  const kafkaUser = response.payload!;
  const kafkaUserName = kafkaUser.attributes.name;

  return <Header params={{ kafkaId, userId }} kafkaUserName={kafkaUserName} />;
}

async function Header({
  kafkaUserName = "",
}: {
  params: KafkaUserParams;
  kafkaUserName?: string;
}) {
  const t = await getTranslations();

  return (
    <AppHeader
      title={
        decodeURIComponent(kafkaUserName) === "+" ? (
          <RichText>{(tags) => t.rich("common.empty_name", tags)}</RichText>
        ) : (
          <>{kafkaUserName}</>
        )
      }
    />
  );
}
