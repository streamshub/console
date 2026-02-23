import RichText from "@/components/RichText";
import {
  Alert,
  Button,
  Card,
  ClipboardCopy,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  JumpLinks,
  JumpLinksItem,
  List,
  ListItem,
  Panel,
  PanelMain,
  PanelMainBody,
  Sidebar,
  SidebarContent,
  SidebarPanel,
  Stack,
  StackItem,
  CardBody,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export type NewOffset = {
  topicName: string;
  partition: number;
  offset: number | string | null;
  metadata?: string;
};

export function Dryrun({
  consumerGroupName,
  newOffset,
  onClickCloseDryrun,
  cliCommand,
}: {
  consumerGroupName: string;
  newOffset: NewOffset[];
  onClickCloseDryrun: () => void;
  cliCommand: string;
}) {
  const t = useTranslations("ConsumerGroupsTable");

  const hasOffsets = newOffset && newOffset.length > 0;

  // Group offsets by topic
  const groupedTopics = newOffset.reduce<Record<string, NewOffset[]>>(
    (acc, offset) => {
      if (!acc[offset.topicName]) {
        acc[offset.topicName] = [];
      }
      acc[offset.topicName].push(offset);
      return acc;
    },
    {},
  );

  return (
    <Panel>
      <PanelMain>
        <PanelMainBody>
          <Stack hasGutter>
            <StackItem>
              <ClipboardCopy isReadOnly hoverTip="Copy" clickTip="Copied">
                {cliCommand}
              </ClipboardCopy>
            </StackItem>
            <StackItem>
              {!hasOffsets ? (
                <Alert variant="warning" isInline title={t("no_offsets_found")}>
                  {t("no_offsets_found_description")}
                </Alert>
              ) : (
                <Sidebar>
                  {Object.keys(groupedTopics).length >= 3 && (
                    <SidebarPanel>
                      <JumpLinks
                        isVertical
                        label={
                          <RichText>
                            {(tags) => t.rich("jump_to_topic", tags)}
                          </RichText>
                        }
                        offset={10}
                      >
                        {Object.keys(groupedTopics).map(
                          (topicName) =>
                            topicName && (
                              <JumpLinksItem
                                key={topicName}
                                href={`#${topicName}`}
                              >
                                {topicName}
                              </JumpLinksItem>
                            ),
                        )}
                      </JumpLinks>
                    </SidebarPanel>
                  )}
                  <SidebarContent
                    style={{ overflowY: "auto", maxHeight: "500px" }}
                  >
                    <Flex
                      direction={{ default: "column" }}
                      spaceItems={{ default: "spaceItemsXl" }}
                    >
                      {Object.entries(groupedTopics).map(
                        ([topicName, offsets]) => (
                          <FlexItem key={topicName}>
                            <Card component="div">
                              <CardBody>
                                <DescriptionList id={`${topicName}`}>
                                  <DescriptionListGroup>
                                    <DescriptionListTerm>
                                      {t("topic")}
                                    </DescriptionListTerm>
                                    <DescriptionListDescription>
                                      {topicName}
                                    </DescriptionListDescription>
                                  </DescriptionListGroup>
                                  <Flex>
                                    <FlexItem>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>
                                          {t("partition")}
                                        </DescriptionListTerm>
                                        <DescriptionListDescription>
                                          <List isPlain>
                                            {offsets
                                              .sort(
                                                (a, b) =>
                                                  a.partition - b.partition,
                                              )
                                              .map(({ partition }) => (
                                                <ListItem key={partition}>
                                                  {partition}
                                                </ListItem>
                                              ))}
                                          </List>
                                        </DescriptionListDescription>
                                      </DescriptionListGroup>
                                    </FlexItem>
                                    <FlexItem>
                                      <DescriptionListGroup>
                                        <DescriptionListTerm>
                                          {t("new_offset")}
                                        </DescriptionListTerm>
                                        <DescriptionListDescription>
                                          <List isPlain>
                                            {offsets.map(
                                              ({ partition, offset }) => (
                                                <ListItem key={partition}>
                                                  {offset ??
                                                    t("offset_deleted")}
                                                </ListItem>
                                              ),
                                            )}
                                          </List>
                                        </DescriptionListDescription>
                                      </DescriptionListGroup>
                                    </FlexItem>
                                  </Flex>
                                </DescriptionList>
                              </CardBody>
                            </Card>
                          </FlexItem>
                        ),
                      )}
                    </Flex>
                  </SidebarContent>
                </Sidebar>
              )}
            </StackItem>
            <StackItem>
              <Alert
                variant="info"
                isInline
                title={t("dry_run_execution_alert")}
              />
            </StackItem>
            <StackItem>
              <Button variant="secondary" onClick={onClickCloseDryrun}>
                {t("back_to_edit_offset")}
              </Button>
            </StackItem>
          </Stack>
        </PanelMainBody>
      </PanelMain>
    </Panel>
  );
}
