import { getTranslations } from "next-intl/server";
import { ApiError } from "@/api/api";
import { getConsumerGroup } from "@/api/groups/actions";
import { ConfigMap } from "@/api/groups/schema";
import { GroupParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/Group.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata(props: { params: { kafkaId: string, groupId: string} }) {
  const t = await getTranslations();

  return {
    title: `${t("Group.title")} ${props.params.groupId} | ${t("common.title")}`,
  };
}

export default async function NodeDetails({
  params: { kafkaId, groupId },
}: {
  params: GroupParams;
}) {
  const response = await getConsumerGroup(kafkaId, groupId, { fields: "groupId,type,protocol,state,configs" });

  if (response.errors) {
    return <NoDataErrorState errors={response.errors!} />;
  } else if (response.payload?.attributes.configs?.meta?.type === "error") {
    // API returns the `configs` property as an error when an error occurs reading configuration
    return <NoDataErrorState errors={[ response.payload?.attributes.configs! as ApiError ]} />;
  } else if (response.payload) {
    return <PageSection isFilled={true}>
      <ConfigTable config={response.payload.attributes.configs as ConfigMap ?? {}} />
    </PageSection>;
  }
}
