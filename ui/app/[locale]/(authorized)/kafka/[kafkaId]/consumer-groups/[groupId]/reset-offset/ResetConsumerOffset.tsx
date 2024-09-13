"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  DateTimeFormatSelection,
  OffsetValue,
  TopicSelection,
  partitionSelection,
} from "../types";
import { useRouter } from "@/navigation";
import {
  getDryrunResult,
  updateConsumerGroup,
} from "@/api/consumerGroups/actions";
import { UpdateConsumerGroupErrorSchema } from "@/api/consumerGroups/schema";
import { Dryrun } from "./Dryrun";
import { LoadingPage } from "./LoadingPage";
import { ResetOffset } from "./ResetOffset";
import { Page } from "@/libs/patternfly/react-core";

export type Offset = {
  topicId: string;
  topicName: string;
  partition: number;
  offset: string | number;
  metadeta?: string;
};

export function ResetConsumerOffset({
  kafkaId,
  consumerGroupName,
  topics,
  partitions,
  baseurl,
}: {
  kafkaId: string;
  consumerGroupName: string;
  topics: { topicId: string; topicName: string }[];
  partitions: number[];
  baseurl: string;
}) {
  const router = useRouter();

  const [selectedConsumerTopic, setSelectedConsumerTopic] =
    useState<TopicSelection>("allTopics");

  const [selectedPartition, setSelectedPartition] =
    useState<partitionSelection>("allPartitions");

  const [selectedOffset, setSelectedOffset] = useState<OffsetValue>("custom");

  const [offset, setOffset] = useState<Offset>({
    topicId: "",
    partition: 0,
    offset: "",
    topicName: "",
  });

  const [selectDateTimeFormat, setSelectDateTimeFormat] =
    useState<DateTimeFormatSelection>("ISO");

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

  const onOffsetSelect = (value: OffsetValue) => setSelectedOffset(value);

  const onDateTimeSelect = (value: DateTimeFormatSelection) => {
    setSelectDateTimeFormat(value);
  };

  const handleTopicChange = (topicName: string | number) => {
    if (typeof topicName === "string") {
      const selectedTopic = topics.find(
        (topic) => topic.topicName === topicName,
      );
      if (selectedTopic) {
        setOffset((prev) => ({
          ...prev,
          topicName: selectedTopic.topicName,
          topicId: selectedTopic.topicId,
        }));
      } else {
        console.warn(
          "Selected topic name not found in topics array:",
          topicName,
        );
      }
    } else {
      console.warn("Expected a string, but got a number:", topicName);
    }
  };

  const handlePartitionChange = (partition: string | number) => {
    if (typeof partition === "number") {
      setOffset((prev) => ({ ...prev, partition }));
    } else {
      console.warn("Expected a number, but got a string:", partition);
    }
  };

  const handleOffsetChange = (value: string) => {
    const numericValue = Number(value);
    setOffset((prev) => ({
      ...prev,
      offset: isNaN(numericValue) ? value : numericValue,
    }));
  };

  const generateCliCommand = (): string => {
    let baseCommand = `$ kafka-consumer-groups --bootstrap-server \${bootstrap-Server} --group ${consumerGroupName} --reset-offsets`;
    const topic =
      selectedConsumerTopic === "allTopics"
        ? "--all-topics"
        : `--topic ${offset.topicName}`;
    baseCommand += ` ${topic}`;
    if (selectedConsumerTopic === "selectedTopic") {
      // Only include partition if a specific topic is selected
      const partition =
        selectedPartition === "allPartitions" ? "" : `:${offset.partition}`;
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

  const generateOffsets = (): Array<{
    topicId: string;
    partition?: number;
    offset: string | number;
  }> => {
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
            offset:
              selectedOffset === "custom" ||
              selectedOffset === "specificDateTime"
                ? selectDateTimeFormat === "Epoch"
                  ? convertEpochToISO(String(offset.offset))
                  : offset.offset
                : selectedOffset,
          });
        });
      });
    } else if (selectedConsumerTopic === "selectedTopic") {
      const uniquePartitions = new Set(
        partitions.map((partition) =>
          selectedPartition === "allPartitions" ? partition : offset.partition,
        ),
      );

      Array.from(uniquePartitions).forEach((partition) => {
        offsets.push({
          topicId: offset.topicId,
          partition,
          offset:
            selectedOffset === "custom" || selectedOffset === "specificDateTime"
              ? selectDateTimeFormat === "Epoch"
                ? convertEpochToISO(String(offset.offset))
                : offset.offset
              : selectedOffset,
        });
      });
    }

    // Remove duplicate entries
    return offsets.filter(
      (value, index, self) =>
        index ===
        self.findIndex(
          (t) => t.topicId === value.topicId && t.partition === value.partition,
        ),
    );
  };

  const isDryRunDisable = !selectedConsumerTopic || !selectedOffset;

  const openDryrun = async () => {
    const uniqueOffsets = generateOffsets();
    const res = await getDryrunResult(
      kafkaId,
      consumerGroupName,
      uniqueOffsets,
    );
    setNewOffsetData(res?.attributes?.offsets ?? []);
    setShowDryRun(true);
  };

  const closeDryrun = () => {
    setShowDryRun(false);
  };

  const closeResetOffset = () => {
    router.push(`${baseurl}`);
  };

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
      const success = await updateConsumerGroup(
        kafkaId,
        consumerGroupName,
        uniqueOffsets,
      );
      if (success === true) {
        closeResetOffset();
      } else {
        const errorMessages =
          (success as UpdateConsumerGroupErrorSchema)?.errors.map(
            (err) => err.detail,
          ) || [];
        const errorMessage =
          errorMessages.length > 0
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
        <LoadingPage />
      ) : showDryRun ? (
        <Dryrun
          consumerGroupName={consumerGroupName}
          newOffset={newoffsetData}
          onClickCloseDryrun={closeDryrun}
          cliCommand={generateCliCommand()}
        />
      ) : (
        <ResetOffset
          consumerGroupName={consumerGroupName}
          topics={topics}
          partitions={partitions}
          selectTopic={selectedConsumerTopic}
          selectPartition={selectedPartition}
          selectOffset={selectedOffset}
          error={error}
          onTopicSelect={onTopicSelect}
          onPartitionSelect={onPartitionSelect}
          onOffsetSelect={onOffsetSelect}
          offset={offset}
          isLoading={isLoading}
          selectDateTimeFormat={selectDateTimeFormat}
          onDateTimeSelect={onDateTimeSelect}
          handleTopichange={handleTopicChange}
          handlePartitionChange={handlePartitionChange}
          handleOffsetChange={handleOffsetChange}
          closeResetOffset={closeResetOffset}
          openDryrun={openDryrun}
          handleDateTimeChange={handleDateTimeChange}
          handleSave={handleSave}
        />
      )}
    </Page>
  );
}
