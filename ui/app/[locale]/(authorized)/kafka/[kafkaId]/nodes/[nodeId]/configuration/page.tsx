import { getTranslations } from "next-intl/server";
import { getNodeConfiguration } from "@/api/nodes/actions";
import { KafkaNodeParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/nodes/kafkaNode.params";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConfigTable } from "./ConfigTable";

export async function generateMetadata(props: { params: { kafkaId: string, nodeId: string  }}) {
  const t = await getTranslations();

  return {
    title: `Broker ${props.params.nodeId} Configuration | ${t("common.title")}`,
  };
}

export default async function NodeDetails({
  params: { kafkaId, nodeId },
}: {
  params: KafkaNodeParams;
}) {
  const config = await getNodeConfiguration(kafkaId, nodeId);
  return (
    <PageSection isFilled={true}>
      <ConfigTable config={config} />
    </PageSection>
  );
}
