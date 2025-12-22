import { updateConsumerGroup } from "@/api/consumerGroups/actions";
import { KafkaConsumerGroupMembersParams } from "../../KafkaConsumerGroupMembers.params";
import { Suspense } from "react";
import { PageSection } from "@/libs/patternfly/react-core";
import { ConnectedDryrunPage } from "./ConnectedDryrunPage";
import { NewOffset } from "./Dryrun";
import { NoDataErrorState } from "@/components/NoDataErrorState";

export default async function DryrunPage(
  props: {
    params: Promise<KafkaConsumerGroupMembersParams>;
    searchParams: Promise<{ data?: string; cliCommand?: string }>;
  }
) {
  const searchParams = await props.searchParams;
  const params = await props.params;

  const {
    kafkaId,
    groupId
  } = params;

  return (
    <PageSection>
      <Suspense
        fallback={
          <ConnectedDryrunPage
            groupId={groupId}
            offsetvalue={[]}
            baseurl={""}
            cliCommand={""}
          />
        }
      >
        <AsyncConnectedDryrunPage
          params={{ kafkaId, groupId }}
          searchParams={searchParams}
        />
      </Suspense>
    </PageSection>
  );
}

async function AsyncConnectedDryrunPage({
  params: { kafkaId, groupId },
  searchParams,
}: {
  params: KafkaConsumerGroupMembersParams;
  searchParams: { data?: string; cliCommand?: string };
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
  const offsets: NewOffset[] = Array.from(res.attributes?.offsets ?? []).map(
    (o) => ({
      topicId: o.topicId!,
      topicName: o.topicName,
      partition: o.partition,
      offset: o.offset,
    }),
  );

  return (
    <ConnectedDryrunPage
      groupId={groupId}
      offsetvalue={offsets}
      baseurl={`/kafka/${kafkaId}/consumer-groups/${groupId}/reset-offset`}
      cliCommand={searchParams?.cliCommand || ""}
    />
  );
}
