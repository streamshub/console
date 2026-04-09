import { getTranslations } from "next-intl/server";
import { ApiError } from "@/api/api";
import { getConsumerGroup } from "@/api/groups/actions";
import { ConfigMap } from "@/api/groups/schema";
import { GroupParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/groups/[groupId]/Group.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export async function generateMetadata({
  params: paramsPromise,
} : {
  params: Promise<GroupParams>;
}) {
  const { kafkaId, groupId } = await paramsPromise;
  const t = await getTranslations();
  const group = (await getConsumerGroup(kafkaId, groupId)).payload;
  let groupIdDisplay = "";

  if (group) {
    groupIdDisplay = group.attributes.groupId;
  }

  return {
    title: `${t("Group.title")} ${groupIdDisplay} | ${t("common.title")}`,
  };
}

export default async function NodeDetails({
  params: paramsPromise,
}: {
  params: Promise<GroupParams>;
}) {
  const { kafkaId, groupId } = await paramsPromise;
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
