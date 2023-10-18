import { ConfigSchemaMap, TopicCreateError } from "@/api/topics";
import { Number } from "@/components/Number";
import {
  Alert,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Grid,
  GridItem,
  Icon,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";
import { ExclamationCircleIcon } from "@patternfly/react-icons";

export function StepReview({
  name,
  nameInvalid,
  partitions,
  partitionsInvalid,
  replicas,
  replicasInvalid,
  options,
  error,
}: {
  name: string;
  nameInvalid: boolean;
  partitions: number;
  partitionsInvalid: boolean;
  replicas: number;
  replicasInvalid: boolean;
  options: ConfigSchemaMap;
  error: TopicCreateError | "unknown" | undefined;
}) {
  const optionEntries = Object.entries(options);
  return (
    <Grid hasGutter={true}>
      <GridItem>
        <Title headingLevel={"h2"}>Review your topic</Title>
      </GridItem>
      {error &&
        (error !== "unknown" ? (
          error.errors.map((e, idx) => (
            <Alert key={idx} title={e.title} variant={"danger"}>
              {e.detail}
            </Alert>
          ))
        ) : (
          <Alert title={"Unexpected error"} variant={"danger"}>
            Sorry, something went wrong. Please try again later.
          </Alert>
        ))}
      <GridItem>
        <Title headingLevel={"h3"}>Topic details</Title>
      </GridItem>
      <GridItem>
        <DescriptionList isHorizontal>
          <DescriptionListGroup>
            <DescriptionListTerm
              icon={
                nameInvalid && (
                  <Icon status={"danger"}>
                    <ExclamationCircleIcon />
                  </Icon>
                )
              }
            >
              Name
            </DescriptionListTerm>
            <DescriptionListDescription>
              {name || <i>empty</i>}
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm
              icon={
                partitionsInvalid && (
                  <Icon status={"danger"}>
                    <ExclamationCircleIcon />
                  </Icon>
                )
              }
            >
              Partitions
            </DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={partitions} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm
              icon={
                replicasInvalid && (
                  <Icon status={"danger"}>
                    <ExclamationCircleIcon />
                  </Icon>
                )
              }
            >
              Replicas
            </DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={replicas} />
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </GridItem>
      <GridItem>
        <Title headingLevel={"h3"}>Advanced options</Title>
      </GridItem>
      <GridItem>
        {optionEntries.length > 0 ? (
          <DescriptionList isHorizontal>
            {optionEntries.map(([name, property], idx) => (
              <DescriptionListGroup key={idx}>
                <DescriptionListTerm>{name}</DescriptionListTerm>
                <DescriptionListDescription>
                  {property.value}
                </DescriptionListDescription>
              </DescriptionListGroup>
            ))}
          </DescriptionList>
        ) : (
          <TextContent>
            <Text component={"small"}>No advanced options specified.</Text>
          </TextContent>
        )}
      </GridItem>
    </Grid>
  );
}
