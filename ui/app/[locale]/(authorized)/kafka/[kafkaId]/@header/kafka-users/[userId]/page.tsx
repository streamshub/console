import { AppHeader } from "@/components/AppHeader";
import { useTranslations } from "next-intl";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { Suspense } from "react";
import RichText from "@/components/RichText";
import { KafkaUserParams } from "../../../kafka-users/kafkaUser.params";
import { getKafkaUser } from "@/api/kafkaUsers/action";

export default function Page({
  params: { kafkaId, userId },
}: {
  params: KafkaUserParams;
}) {
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

function Header({
  kafkaUserName = "",
}: {
  params: KafkaUserParams;
  kafkaUserName?: string;
}) {
  const t = useTranslations();

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
