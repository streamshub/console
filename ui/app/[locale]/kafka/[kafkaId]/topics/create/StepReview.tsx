import { ConfigMap, NewConfigMap, TopicMutateError } from "@/api/topics/schema";
import { Error } from "@/app/[locale]/kafka/[kafkaId]/topics/create/Errors";
import { ReviewTable } from "@/app/[locale]/kafka/[kafkaId]/topics/create/ReviewTable";
import { Number } from "@/components/Format/Number";
import {
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";

export function StepReview({
  name,
  partitions,
  replicas,
  options,
  initialOptions,
  error,
}: {
  name: string;
  partitions: number;
  replicas: number;
  options: NewConfigMap;
  initialOptions: ConfigMap;
  error: TopicMutateError | "unknown" | undefined;
}) {
  const optionEntries = Object.entries(options);
  return (
    <Grid hasGutter={true}>
      <GridItem>
        <Title headingLevel={"h2"}>Review your topic</Title>
      </GridItem>
      <GridItem>
        <Title headingLevel={"h3"}>Topic details</Title>
      </GridItem>
      {error && <Error error={error} />}
      <GridItem>
        <DescriptionList isHorizontal>
          <DescriptionListGroup>
            <DescriptionListTerm>Name</DescriptionListTerm>
            <DescriptionListDescription>
              {name || <i>empty</i>}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Partitions</DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={partitions} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>Replicas</DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={replicas} />
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </GridItem>
      <GridItem>
        <Title headingLevel={"h3"}>Options</Title>
      </GridItem>
      <GridItem>
        {optionEntries.length > 0 ? (
          <ReviewTable options={options} initialOptions={initialOptions} />
        ) : (
          <TextContent>
            <Text component={"small"}>No advanced options specified.</Text>
          </TextContent>
        )}
      </GridItem>
    </Grid>
  );
}
