import { useTranslations } from "next-intl";
import { Suspense } from "react";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { KafkaUserDetailsBreadcrumb } from "./KafkaUserDetailsBreadcrumb";
import { getKafkaUser } from "@/api/kafkaUsers/action";
import { KafkaUserParams } from "../../../kafka-users/kafkaUser.params";

export default async function Page(
  props: {
    params: Promise<KafkaUserParams>;
  }
) {
  const params = await props.params;

  const {
    kafkaId,
    userId
  } = params;

  return (
    <Suspense
      fallback={<KafkaUserActiveBreadcrumb params={{ kafkaId, userId }} />}
    >
      <KafkaUserDetailsActiveBreadcrumb
        params={{
          kafkaId: kafkaId,
          userId: userId,
        }}
      />
    </Suspense>
  );
}

async function KafkaUserDetailsActiveBreadcrumb({
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

  return (
    <KafkaUserActiveBreadcrumb
      params={{
        kafkaId: kafkaId,
        userId: userId,
      }}
      name={kafkaUserName}
    />
  );
}

function KafkaUserActiveBreadcrumb({
  params: { kafkaId, userId },
  name = "",
}: {
  params: KafkaUserParams;
  name?: string;
}) {
  const t = useTranslations();

  return <KafkaUserDetailsBreadcrumb kafkaId={kafkaId} name={name} />;
}
