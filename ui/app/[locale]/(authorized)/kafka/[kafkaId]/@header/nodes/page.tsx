import { getKafkaCluster } from "@/api/kafka/actions";
import { KafkaParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/kafka.params";
import { AppHeader } from "@/components/AppHeader";
import { Number } from "@/components/Format/Number";
import {
  Label,
  PageSection,
  Spinner,
  Split,
  SplitItem,
  Tooltip,
} from "@/libs/patternfly/react-core";
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
} from "@/libs/patternfly/react-icons";
import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { NodesTabs } from "./NodesTabs";

export default async function NodesHeader(props: { params: Promise<KafkaParams> }) {
  const params = await props.params;
  return (
    <Suspense
      fallback={<Header kafkaId={undefined} cruiseControlEnable={false} />}
    >
      <ConnectedHeader params={params} />
    </Suspense>
  );
}

async function ConnectedHeader({ params }: { params: KafkaParams }) {
  const cluster = (await getKafkaCluster(params.kafkaId))?.payload;
  const combinedStatuses =
    cluster?.relationships.nodes?.meta?.summary?.statuses?.combined || {};

  return (
    <Header
      total={Object.values(combinedStatuses).reduce(
        (sum, count) => sum + count,
        0,
      )}
      ok={combinedStatuses["Healthy"] ?? 0}
      warning={combinedStatuses["Unhealthy"] ?? 0}
      kafkaId={cluster?.id}
      cruiseControlEnable={cluster?.attributes.cruiseControlEnabled || false}
    />
  );
}

function Header({
  total,
  ok,
  warning,
  kafkaId,
  cruiseControlEnable,
}: {
  total?: number;
  ok?: number;
  warning?: number;
  kafkaId: string | undefined;
  cruiseControlEnable: boolean;
}) {
  const t = useTranslations("node-header");

  return (
    <AppHeader
      title={
        <Split hasGutter={true}>
          <SplitItem>{t("title")}</SplitItem>
          <SplitItem>
            <Label
              icon={total === undefined ? <Spinner size={"sm"} /> : undefined}
            >
              {total !== undefined && <Number value={total} />}&nbsp;total
            </Label>
          </SplitItem>
          <SplitItem>
            <Tooltip content={"Number of healthy nodes"}>
              <Label
                icon={
                  ok === undefined ? (
                    <Spinner size={"sm"} />
                  ) : (
                    <CheckCircleIcon />
                  )
                }
                color={"teal"}
              >
                {ok !== undefined && <Number value={ok} />}
              </Label>
            </Tooltip>
          </SplitItem>
          <SplitItem>
            <Tooltip content={"Number of unhealthy nodes"}>
              <Label
                icon={
                  warning === undefined ? (
                    <Spinner size={"sm"} />
                  ) : (
                    <ExclamationTriangleIcon />
                  )
                }
                color={"orange"}
              >
                {warning !== undefined && <Number value={warning} />}
              </Label>
            </Tooltip>
          </SplitItem>
        </Split>
      }
      navigation={
        <PageSection className={"pf-v6-u-px-sm"} type="subnav">
          <NodesTabs
            kafkaId={kafkaId}
            cruiseControlEnable={cruiseControlEnable}
          />
        </PageSection>
      }
    />
  );
}
