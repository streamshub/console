"use client";

import { Divider, Panel, PanelHeader, PanelMain, PanelMainBody, TextContent, Text, TextVariants, Radio, Form, FormGroup, FormSection, Select, SelectList, SelectOption, MenuToggle, MenuToggleElement, TextInput, ActionGroup, Button, SelectProps, PageSection, Page, Bullseye, Spinner, Flex, FlexItem, Grid, GridItem, Alert } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { DateTimeFormatSelection, OffsetValue, TopicSelection, partitionSelection } from "../types";
import { TypeaheadSelect } from "./TypeaheadSelect";
import { OffsetSelect } from "./OffsetSelect";
import { useRouter } from "@/navigation";
import { getDryrunResult, updateConsumerGroup } from "@/api/consumerGroups/actions";
import { UpdateConsumerGroupErrorSchema } from "@/api/consumerGroups/schema";
import { Dryrun } from "./Dryrun";

export type Offset = {
  topicId: string
  topicName: string;
  partition: number;
  offset: string | number;
  metadeta?: string
};

export function ResetConsumerOffset({
  kafkaId,
  consumerGroupName,
  topics,
  partitions,
  baseurl

}: {
  kafkaId: string;
  consumerGroupName: string;
  topics: { topicId: string, topicName: string }[];
  partitions: number[];
  baseurl: string;
}) {
  const t = useTranslations("ConsumerGroupsTable");

  const router = useRouter();

  const [selectedConsumerTopic, setSelectedConsumerTopic] = useState<TopicSelection>();

  const [selectedPartition, setSelectedPartition] = useState<partitionSelection>();

  const [selectedOffset, setSelectedOffset] = useState<OffsetValue>("custom");

  const [offset, setOffset] = useState<Offset>({
    topicId: "",
    partition: 0,
    offset: "",
    topicName: ""
  });

  const [selectDateTimeFormat, setSelectDateTimeFormat] = useState<DateTimeFormatSelection>("ISO");

  const [isLoading, setIsLoading] = useState(false);

  const [error, setError] = useState<string | undefined>();

  const [newoffsetData, setNewOffsetData] = useState<Offset[]>([]);

  const [showDryRun, setShowDryRun] = useState(false);

  const onTopicSelect = (value: TopicSelection) => {
    setSelectedConsumerTopic(value);
  };

  const onPartitionSelect = (value: partitionSelection) => {
    setSelectedPartition(value);
  };

  const onDateTimeSelect = (value: DateTimeFormatSelection) => {
    setSelectDateTimeFormat(value)
  }

  const handleTopicChange = (topicName: string | number) => {
    if (typeof topicName === 'string') {
      const selectedTopic = topics.find(topic => topic.topicName === topicName);
      if (selectedTopic) {
        setOffset((prev) => ({
          ...prev,
          topicName: selectedTopic.topicName,
          topicId: selectedTopic.topicId,
        }));
      } else {
        console.warn('Selected topic name not found in topics array:', topicName);
      }
    } else {
      console.warn('Expected a string, but got a number:', topicName);
    }
  };

  const handlePartitionChange = (partition: string | number) => {
    if (typeof partition === 'number') {
      setOffset((prev) => ({ ...prev, partition }));
    } else {
      console.warn('Expected a number, but got a string:', partition
      );
    }
  };

  const handleOffsetChange = (value: string) => {
    const numericValue = Number(value);
    setOffset((prev) => ({ ...prev, offset: isNaN(numericValue) ? value : numericValue }));
  };

  const generateCliCommand = (): string => {
    let baseCommand = `$ kafka-consumer-groups --bootstrap-server localhost:9092 --group ${consumerGroupName} --reset-offsets`;
    const topic = selectedConsumerTopic === "allTopics" ? "--all-topics" : `--topic ${offset.topicName}`;
    baseCommand += ` ${topic}`;
    if (selectedConsumerTopic === "selectedTopic") {
      // Only include partition if a specific topic is selected
      const partition = selectedPartition === "allPartitions" ? "" : `:${offset.partition}`;
      baseCommand += `${partition}`;
    }
    if (selectedOffset === "custom") {
      baseCommand += ` --to-offset ${offset.offset}`;
    } else if (selectedOffset === "specificDateTime") {
      baseCommand += ` --to-datetime ${offset.offset}`;
    } else {
      baseCommand += ` --to-${selectedOffset}`;
    }
    baseCommand += ` --dry-run`;
    return baseCommand;
  };

  const generateOffsets = (): Array<{ topicId: string; partition?: number; offset: string | number }> => {
    const offsets: Array<{
      topicId: string;
      partition?: number;
      offset: string | number;
    }> = [];

    if (selectedConsumerTopic === "allTopics") {
      topics.forEach((topic) => {
        partitions.forEach((partition) => {
          offsets.push({
            topicId: topic.topicId,
            partition: partition,
            offset: selectedOffset === "custom" || selectedOffset === "specificDateTime"
              ? selectDateTimeFormat === "Epoch"
                ? convertEpochToISO(String(offset.offset))
                : offset.offset
              : selectedOffset,
          });
        });
      });
    } else if (selectedConsumerTopic === "selectedTopic") {
      const uniquePartitions = new Set(
        partitions.map(partition => selectedPartition === "allPartitions" ? partition : offset.partition)
      );

      Array.from(uniquePartitions).forEach((partition) => {
        offsets.push({
          topicId: offset.topicId,
          partition,
          offset: selectedOffset === "custom" || selectedOffset === "specificDateTime"
            ? selectDateTimeFormat === "Epoch"
              ? convertEpochToISO(String(offset.offset))
              : offset.offset
            : selectedOffset,
        });
      });
    }

    // Remove duplicate entries
    return offsets.filter((value, index, self) =>
      index === self.findIndex((t) => (
        t.topicId === value.topicId && t.partition === value.partition
      ))
    );
  };

  const isDryRunDisable =
    !selectedConsumerTopic ||
    !selectedOffset

  const openDryrun = async () => {
    const uniqueOffsets = generateOffsets();
    const res = await getDryrunResult(kafkaId, consumerGroupName, uniqueOffsets);
    setNewOffsetData(res?.attributes?.offsets ?? []);
    setShowDryRun(true)
  }

  const closeDryrun = () => {
    setShowDryRun(false)
  }

  const closeResetOffset = () => {
    router.push(`${baseurl}`)
  }

  const handleDateTimeChange = (value: string) => {
    setOffset((prev) => ({ ...prev, offset: value }));
  };

  const convertEpochToISO = (epoch: string): string => {
    const date = new Date(parseInt(epoch, 10));
    return date.toISOString();
  };


  const handleSave = async () => {
    setError(undefined);
    setIsLoading(true);

    try {
      const uniqueOffsets = generateOffsets();
      const success = await updateConsumerGroup(kafkaId, consumerGroupName, uniqueOffsets);
      if (success === true) {
        closeResetOffset();
      } else {
        const errorMessages = (success as UpdateConsumerGroupErrorSchema)?.errors.map((err) => err.detail) || [];
        const errorMessage = errorMessages.length > 0
          ? errorMessages[0]
          : "Failed to update consumer group";
        setError(errorMessage);
      }
    } catch (e: unknown) {
      setError("Unknown error occurred");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Page>
      {isLoading ? (
        <Grid hasGutter={true}>
          <GridItem>
            <Bullseye>
              <Spinner size="xl" aria-label={t("resetting_spinner")}
                aria-valuetext={t("resetting_spinner")} />
            </Bullseye>
          </GridItem>
          <GridItem>
            <Bullseye>
              {t("reseting_consumer_group_offsets_text")}
            </Bullseye>
          </GridItem>
        </Grid>) : showDryRun ? (
          <Dryrun
            consumerGroupName={consumerGroupName}
            newOffset={newoffsetData}
            onClickCloseDryrun={closeDryrun}
            cliCommand={generateCliCommand()}
          />
        ) :
        (
          <Panel>
            <PanelHeader>
              <TextContent>
                <Text component={TextVariants.h1}>{t("reset_consumer_offset")}</Text>
              </TextContent>
              <TextContent>
                <Text>{t.rich("consumer_name", { consumerGroupName })}</Text>
              </TextContent>
            </PanelHeader>
            <Divider />
            <PanelMain>
              <PanelMainBody>
                {error && (
                  <Alert variant="danger" isInline title={error} />
                )}
                <Form>
                  <FormSection title={t("target")}>
                    <FormGroup role="radiogroup" isInline fieldId="select-consumer" hasNoPaddingTop label={t("apply_action_on")}>
                      <Radio name={"consumer-topic-select"} id={"all-consumer-topic"} label={t("all_consumer_topics")}
                        isChecked={selectedConsumerTopic === "allTopics"}
                        onChange={() => onTopicSelect("allTopics")} />
                      <Radio name={"consumer-topic-select"} id={"selected-topic"} label={t("selected_topic")} isChecked={selectedConsumerTopic === "selectedTopic"}
                        onChange={() => onTopicSelect("selectedTopic")} />
                    </FormGroup>
                    {selectedConsumerTopic === "selectedTopic" && (
                      <TypeaheadSelect
                        value={offset.topicName || ""}
                        selectItems={topics.map(topic => topic.topicName)}
                        onChange={handleTopicChange} placeholder={"Select topic"} />
                    )}
                    {selectedConsumerTopic === "selectedTopic" && <FormGroup label={t("partitions")} isInline>
                      <Radio name={"partition-select"} id={"all-partitions"} label={t("all_partitions")}
                        isChecked={selectedPartition === "allPartitions"}
                        onChange={() => onPartitionSelect("allPartitions")} />
                      <Radio name={"partition-select"} id={"selected_partition"} label={t("selected_partition")}
                        isChecked={selectedPartition === "selectedPartition"}
                        onChange={() => onPartitionSelect("selectedPartition")} />
                    </FormGroup>
                    }
                    {selectedConsumerTopic === "selectedTopic" && selectedPartition === "selectedPartition" && (
                      <TypeaheadSelect
                        value={offset.partition}
                        selectItems={partitions}
                        onChange={handlePartitionChange}
                        placeholder={"Select partition"}
                      />
                    )}
                  </FormSection>
                  <FormSection title={t("offset_details")}>
                    <FormGroup label={t("new_offset")}>
                      <OffsetSelect
                        value={selectedOffset}
                        onChange={setSelectedOffset} />
                    </FormGroup>
                    {selectedOffset === "custom" &&
                      <FormGroup
                        label={t("custom_offset")}
                        fieldId="custom-offset-input"
                      >
                        <TextInput
                          id="custom-offset-input"
                          name={t("custom_offset")}
                          value={offset.offset}
                          onChange={(_event, value) => handleOffsetChange(value)}
                          type="number"
                        />
                      </FormGroup>}
                    {selectedOffset === "specificDateTime" &&
                      <>
                        <FormGroup role="radiogroup" isInline fieldId="select-consumer" hasNoPaddingTop label={t("select_date_time")}>
                          <Radio name={"select_time"} id={"iso_date_format"} label={t("iso_date_format")}
                            isChecked={selectDateTimeFormat === "ISO"}
                            onChange={() => onDateTimeSelect("ISO")} />
                          <Radio name={"select_time"} id={"unix_date_format"} label={t("unix_date_format")}
                            isChecked={selectDateTimeFormat === "Epoch"}
                            onChange={() => onDateTimeSelect("Epoch")} />
                        </FormGroup>
                        <FormGroup>
                          <TextInput
                            id="date-input"
                            name={"date-input"}
                            type={selectDateTimeFormat === "ISO" ? "text" : "number"}
                            placeholder={selectDateTimeFormat === "ISO" ? "yyyy-MM-dd'T'HH:mm:ss.SSS" : "specify epoch timestamp"}
                            onChange={(_event, value) => handleDateTimeChange(value)} />
                        </FormGroup>
                      </>}
                  </FormSection>
                  <ActionGroup>
                    <Button variant="primary" onClick={handleSave} isDisabled={isLoading || isDryRunDisable}>{t("save")}</Button>
                    <Button variant="secondary" onClick={openDryrun} isDisabled={isLoading || isDryRunDisable}>{t("dry_run")}</Button>
                    <Button variant="link" onClick={closeResetOffset} isDisabled={isLoading}>{t("cancel")}</Button>
                  </ActionGroup>
                </Form>
              </PanelMainBody>
            </PanelMain>
          </Panel >)
      }
    </Page >
  )
}
