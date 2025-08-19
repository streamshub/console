import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { PageSection } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { KafkaConnectTabs } from "./KafkaConnectTabs";

export default function ConnectorsHeader({ params }: { params: KafkaParams }) {
  return (
    <AppHeader
      title={"Kafka Connect"}
      navigation={
        <PageSection className={"pf-v6-u-px-sm"} type="subnav">
          <KafkaConnectTabs kafkaId={params.kafkaId} />
        </PageSection>
      }
    />
  );
}
