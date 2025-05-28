import { KafkaConsumerGroupMembersParams } from "@/app/[locale]/(authorized)/kafka/[kafkaId]/consumer-groups/[groupId]/KafkaConsumerGroupMembers.params";
import { AppHeader } from "@/components/AppHeader";
import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { Flex, FlexItem, Content } from "@/libs/patternfly/react-core";
import { NoDataErrorState } from "@/components/NoDataErrorState";
import { updateConsumerGroup } from "@/api/consumerGroups/actions";
import { Offset } from "../../../../../consumer-groups/[groupId]/reset-offset/ResetOffset";
import { DryrunDownloadButton } from "./DryrunDownloadButton";
import RichText from "@/components/RichText";

export default function Page({
  params: { kafkaId, groupId },
  searchParams,
}: {
  params: KafkaConsumerGroupMembersParams;
  searchParams: { data?: string };
}) {
  return (
    <Suspense fallback={<Header params={{ kafkaId, groupId }} offsets={[]} />}>
      <ConnectedAppHeader
        params={{ kafkaId, groupId }}
        searchParams={searchParams}
      />
    </Suspense>
  );
}

async function ConnectedAppHeader({
  params: { kafkaId, groupId },
  searchParams,
}: {
  params: KafkaConsumerGroupMembersParams;
  searchParams: { data?: string };
}) {
  const data = searchParams?.data;

  if (!data) {
    const error = {
      title: "Missing Data",
      detail: "Offset data is missing in the query parameters.",
    };

    return <NoDataErrorState errors={[error]} />;
  }

  let parsedData: {
    topicId: string;
    partition?: number;
    offset: string | number;
    metadata?: string;
  }[];

  try {
    parsedData = JSON.parse(data);
    if (!Array.isArray(parsedData)) {
      throw new Error("Parsed data is not an array");
    }
  } catch (err) {
    console.error("Error parsing data from query params:", err);

    const error = {
      title: "Invalid Data",
      detail: "The offset data in the query parameters is invalid.",
    };

    return <NoDataErrorState errors={[error]} />;
  }

  const response = await updateConsumerGroup(
    kafkaId,
    groupId,
    parsedData,
    true,
  );

  if (response.errors) {
    return <NoDataErrorState errors={response.errors!} />;
  }

  const res = response.payload!;
  const offsets: Offset[] = Array.from(res.attributes?.offsets ?? []).map(
    (o) => ({
      topicId: o.topicId!,
      topicName: o.topicName,
      partition: o.partition,
      offset: o.offset,
    }),
  );
  return <Header params={{ kafkaId, groupId }} offsets={offsets} />;
}

function Header({
  params: { kafkaId, groupId },
  offsets,
}: {
  params: KafkaConsumerGroupMembersParams;
  offsets: Offset[];
}) {
  const t = useTranslations("ConsumerGroupsTable");

  return (
    <AppHeader
      title={
        <Flex>
          <FlexItem>
            <Content>
              <Content>
                <RichText>{(tags) => t.rich("dry_run_result", tags)}</RichText>
              </Content>
            </Content>
          </FlexItem>
          <FlexItem>
            <DryrunDownloadButton groupId={groupId} offsets={offsets} />
          </FlexItem>
        </Flex>
      }
      subTitle={
        decodeURIComponent(groupId) === "+" ? (
          <i>Empty Name</i>
        ) : (
          <RichText>
            {(tags) => t.rich("consumer_name", { ...tags, groupId })}
          </RichText>
        )
      }
    />
  );
}
