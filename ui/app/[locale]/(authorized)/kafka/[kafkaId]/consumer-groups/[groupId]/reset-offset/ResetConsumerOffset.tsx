"use client";

import { useState } from "react";
import {
  DateTimeFormatSelection,
  OffsetValue,
  TopicSelection,
  partitionSelection,
} from "../types";
import { useRouter } from "@/i18n/routing";
import { updateConsumerGroup } from "@/api/consumerGroups/actions";
import { LoadingPage } from "./LoadingPage";
import { ResetOffset } from "./ResetOffset";
import { useTranslations } from "next-intl";
import { useAlert } from "@/components/AlertContext";
import { ConsumerGroup } from "@/api/consumerGroups/schema";

export type Offset = {
  topicId: string;
  topicName: string;
  partition: number;
  offset: string | number;
  metadeta?: string;
};

export type ResetOffsetErrorParam =
  | "KafkaError"
  | "CustomOffsetError"
  | "PartitionError"
  | "SpecificDateTimeNotValidError"
  | "GeneralError";

export type ErrorState = Partial<Record<ResetOffsetErrorParam, string>>;

type OffsetAlteration = string | number | null;

export function ResetConsumerOffset({
  kafkaId,
  consumerGroup,
  topics,
  partitions,
  baseurl,
}: {
  kafkaId: string;
  readonly consumerGroup: ConsumerGroup;
  topics: { topicId: string; topicName: string }[];
  partitions: { topicId: string; partitionNumber: number }[];
  baseurl: string;
}) {
  const router = useRouter();
  const t = useTranslations();

  const { addAlert } = useAlert();

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

  const [error, setError] = useState<ErrorState>({});

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
      offset: Number.isNaN(numericValue) ? value : numericValue,
    }));
  };

  const generateCliCommand = (): string => {
    let baseCommand = `$ kafka-consumer-groups --bootstrap-server \${bootstrap-Server} --group '${consumerGroup.attributes.groupId}' `;

    if (selectedOffset === "delete") {
      baseCommand += "--delete-offsets ";
    } else {
      baseCommand += "--reset-offsets ";
    }

    if (selectedConsumerTopic === "allTopics") {
      baseCommand += "--all-topics";
    } else {
      // Only include partition if a specific topic is selected
      const partition =
        selectedPartition === "allPartitions" ? "" : `:${offset.partition}`;

      baseCommand += `--topic ${offset.topicName}${partition}`;
    }

    if (selectedOffset === "custom") {
      baseCommand += ` --to-offset ${offset.offset}`;
    } else if (selectedOffset === "specificDateTime") {
      baseCommand += ` --to-datetime ${offset.offset}`;
    } else if (selectedOffset !== "delete") {
      baseCommand += ` --to-${selectedOffset}`;
    }

    baseCommand += ` --dry-run`;

    return baseCommand;
  };

  const requestOffset = (
    selectedOffset: OffsetValue,
    selectDateTimeFormat: DateTimeFormatSelection,
  ): OffsetAlteration => {
    let reqOffset: OffsetAlteration;

    switch (selectedOffset) {
      case "custom":
        reqOffset = offset.offset;
        break;
      case "delete":
        reqOffset = null;
        break;
      case "specificDateTime":
        if (selectDateTimeFormat === "Epoch") {
          reqOffset = convertEpochToISO(String(offset.offset));
        } else {
          reqOffset = offset.offset;
        }
        break;
      case "earliest":
      case "latest":
      default:
        reqOffset = selectedOffset;
        break;
    }

    return reqOffset;
  };

  const generateOffsets = (): Array<{
    topicId: string;
    partition: number;
    offset: OffsetAlteration;
  }> => {
    const offsets: Array<{
      topicId: string;
      partition: number;
      offset: OffsetAlteration;
    }> = [];

    const requestedOffset = requestOffset(selectedOffset, selectDateTimeFormat);

    if (selectedConsumerTopic === "allTopics") {
      for (let partition of partitions) {
        offsets.push({
          topicId: partition.topicId,
          partition: partition.partitionNumber,
          offset: requestedOffset,
        });
      }
    } else {
      for (let partition of partitions) {
        if (partition.topicId === offset.topicId) {
          if (selectedPartition === "allPartitions" || offset.partition === partition.partitionNumber) {
            offsets.push({
              topicId: offset.topicId,
              partition: partition.partitionNumber,
              offset: requestedOffset,
            });
          }
        }
      }
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

  const openDryrun = () => {
    const uniqueOffsets = generateOffsets();
    const data = JSON.stringify(uniqueOffsets);
    const searchParams = new URLSearchParams();
    const cliCommand = generateCliCommand();
    searchParams.set("data", data);
    searchParams.set("cliCommand", cliCommand);
    router.push(
      `${baseurl}/${consumerGroup.id}/reset-offset/dryrun?${searchParams.toString()}`,
    );
  };

  const closeResetOffset = () => {
    router.push(`${baseurl}`);
  };

  const handleDateTimeChange = (value: string) => {
    setOffset((prev) => ({ ...prev, offset: value }));
  };

  const convertEpochToISO = (epoch: string): string => {
    const date = new Date(Number.parseInt(epoch, 10));
    return date.toISOString();
  };

  const handleSave = async () => {
    setError({});
    setIsLoading(true);
    const groupId = consumerGroup.attributes.groupId;

    try {
      const uniqueOffsets = generateOffsets();
      const response = await updateConsumerGroup(
        kafkaId,
        consumerGroup.id,
        uniqueOffsets,
      );
      if (response.errors?.length) {
        const newErrors: ErrorState = {};

        response.errors.forEach((err) => {
          if (err.detail.includes("must be between the earliest offset")) {
            newErrors.CustomOffsetError = err.detail;
          } else if (err.detail.includes("not valid for topic")) {
            newErrors.PartitionError = err.detail;
          } else if (
            err.detail.includes("does not exist") ||
            err.status === "404"
          ) {
            newErrors.KafkaError = err.detail;
          } else if (err.detail.includes("valid UTC ISO timestamp")) {
            newErrors.SpecificDateTimeNotValidError = err.detail;
          } else {
            newErrors.GeneralError = err.detail;
          }
        });

        setError(newErrors);
      } else {
        closeResetOffset();
        addAlert({
          title: t("ConsumerGroupsTable.reset_offset_submitted_successfully", {
            groupId,
          }),
          variant: "success",
        });
      }
    } catch (e: unknown) {
      setError({ GeneralError: "Unknown error" });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {isLoading ? (
        <LoadingPage />
      ) : (
        <ResetOffset
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
          cliCommand={generateCliCommand()}
        />
      )}
    </>
  );
}
