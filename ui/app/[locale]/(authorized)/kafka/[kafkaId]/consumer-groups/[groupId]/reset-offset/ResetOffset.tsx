import {
  Panel,
  PanelMain,
  PanelMainBody,
  Radio,
  Form,
  FormGroup,
  FormSection,
  TextInput,
  ActionGroup,
  Button,
  Alert,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import {
  DateTimeFormatSelection,
  OffsetValue,
  TopicSelection,
  partitionSelection,
} from "../types";
import { TypeaheadSelect } from "./TypeaheadSelect";
import { DryrunSelect } from "./DryrunSelect";
import { SelectComponent } from "./SelectComponent";
import { ErrorState } from "./ResetConsumerOffset";
import { ExclamationCircleIcon } from "@/libs/patternfly/react-icons";

export type Offset = {
  topicId: string;
  topicName: string;
  partition: number;
  offset: string | number;
  metadeta?: string;
};

export function ResetOffset({
  cliCommand,
  topics,
  partitions,
  selectTopic,
  selectPartition,
  error,
  onTopicSelect,
  selectOffset,
  onPartitionSelect,
  offset,
  isLoading,
  selectDateTimeFormat,
  openDryrun,
  closeResetOffset,
  handleDateTimeChange,
  handleOffsetChange,
  handlePartitionChange,
  handleTopichange,
  handleSave,
  onDateTimeSelect,
  onOffsetSelect,
}: {
  cliCommand: string;
  topics: { topicId: string; topicName: string }[];
  partitions: { topicId: string; partitionNumber: number }[];
  selectTopic: TopicSelection;
  selectPartition: partitionSelection;
  selectOffset: OffsetValue;
  error?: ErrorState;
  onTopicSelect: (value: TopicSelection) => void;
  onPartitionSelect: (value: partitionSelection) => void;
  offset: Offset;
  isLoading: boolean;
  selectDateTimeFormat: DateTimeFormatSelection;
  handleTopichange: (topicName: string | number) => void;
  handlePartitionChange: (partition: number) => void;
  handleOffsetChange: (value: string) => void;
  closeResetOffset: () => void;
  openDryrun: () => void;
  handleDateTimeChange: (value: string) => void;
  handleSave: () => void;
  onDateTimeSelect: (value: DateTimeFormatSelection) => void;
  onOffsetSelect: (value: OffsetValue) => void;
}) {
  const t = useTranslations("ConsumerGroupsTable");
  const isTopicSelected = selectTopic === "selectedTopic";
  const isPartitionSelected = selectPartition === "selectedPartition";
  const hasTopicName = typeof offset.topicName === "string" && offset.topicName.length > 0;
  const hasPartition = typeof offset.partition === "number";
  const hasCustomOffset = typeof offset.offset === "number";
  const hasSpecificDateTime = typeof offset.offset === "string";

  const submitEnabled = {
    specificDateTime: () => hasSpecificDateTime,
    latest: () => true,
    earliest: () => true,
    custom: () =>
      isTopicSelected &&
      hasTopicName &&
      isPartitionSelected &&
      hasCustomOffset,
    delete: () =>
      isTopicSelected &&
      hasTopicName &&
      (selectPartition === "allPartitions" || hasPartition),
  }[selectOffset] ?? false;

  const isEnabled = submitEnabled;

  const offsetOptions: { value: OffsetValue, label: string }[] = [
    ...(isTopicSelected && isPartitionSelected
      ? [{ value: "custom" as OffsetValue, label: t("offset.custom") }]
      : []),

    { value: "earliest", label: t("offset.earliest") },
    { value: "latest", label: t("offset.latest") },
    { value: "specificDateTime", label: t("offset.specific_date_time") },

    ...(isTopicSelected
      ? [{ value: "delete" as OffsetValue, label: t("offset.delete") }]
      : []),
  ];

  return (
    <Panel>
      <PanelMain>
        <PanelMainBody>
          {error?.GeneralError && (
            <Alert variant="danger" isInline title={error.GeneralError} />
          )}
          {error?.PartitionError && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem
                  icon={<ExclamationCircleIcon />}
                  variant={"error"}
                >
                  {error.PartitionError}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
          <Form>
            <FormSection title={t("target")}>
              <FormGroup
                role="radiogroup"
                isInline
                fieldId="select-consumer"
                hasNoPaddingTop
                label={t("apply_action_on")}
              >
                <Radio
                  name={"consumer-topic-select"}
                  id={"all-consumer-topic"}
                  label={t("all_consumer_topics")}
                  isChecked={selectTopic === "allTopics"}
                  onChange={() => onTopicSelect("allTopics")}
                />
                <Radio
                  name={"consumer-topic-select"}
                  id={"selected-topic"}
                  label={t("selected_topic")}
                  isChecked={selectTopic === "selectedTopic"}
                  onChange={() => onTopicSelect("selectedTopic")}
                />
              </FormGroup>
              {selectTopic === "selectedTopic" && (
                <TypeaheadSelect
                  value={offset.topicName || ""}
                  selectItems={topics?.map((topic) => topic.topicName)}
                  onChange={handleTopichange}
                  placeholder={"Select topic"}
                />
              )}
              {selectTopic === "selectedTopic" && (
                <FormGroup label={t("partitions")} isInline>
                  <Radio
                    name={"partition-select"}
                    id={"all-partitions"}
                    label={t("all_partitions")}
                    isChecked={selectPartition === "allPartitions"}
                    onChange={() => onPartitionSelect("allPartitions")}
                  />
                  <Radio
                    name={"partition-select"}
                    id={"selected_partition"}
                    label={t("selected_partition")}
                    isChecked={selectPartition === "selectedPartition"}
                    onChange={() => onPartitionSelect("selectedPartition")}
                  />
                </FormGroup>
              )}
              {selectTopic === "selectedTopic" &&
                selectPartition === "selectedPartition" && (
                  <SelectComponent<number>
                    options={Array.from(new Set(partitions))
                      .sort((a, b) => a.partitionNumber - b.partitionNumber)
                      .map((partition) => ({
                        value: partition.partitionNumber,
                        label: `${partition.partitionNumber}`,
                      }))}
                    value={offset.partition}
                    placeholder={"Select partition"}
                    onChange={handlePartitionChange}
                  />
                )}
            </FormSection>
            <FormSection title={t("offset_details")}>
              <FormGroup label={t("new_offset")}>
                <SelectComponent<OffsetValue>
                  options={offsetOptions}
                  value={selectOffset}
                  onChange={onOffsetSelect}
                  placeholder="Select an offset"
                />
              </FormGroup>
              {selectOffset === "custom" &&
                selectTopic !== "allTopics" &&
                selectPartition !== "allPartitions" && (
                  <FormGroup
                    label={t("custom_offset")}
                    fieldId="custom-offset-input"
                  >
                    <TextInput
                      id="custom-offset-input"
                      name={t("custom_offset")}
                      value={offset.offset}
                      onChange={(_event, value) => {
                        if (/^\d*$/.test(value)) {
                          handleOffsetChange(value);
                        }
                      }}
                      type="number"
                      min={0}
                    />
                    {error?.CustomOffsetError && (
                      <FormHelperText>
                        <HelperText>
                          <HelperTextItem
                            icon={<ExclamationCircleIcon />}
                            variant={"error"}
                          >
                            {error.CustomOffsetError}
                          </HelperTextItem>
                        </HelperText>
                      </FormHelperText>
                    )}
                  </FormGroup>
                )}
              {selectOffset === "specificDateTime" && (
                <>
                  <FormGroup
                    role="radiogroup"
                    isInline
                    fieldId="select-consumer"
                    hasNoPaddingTop
                    label={t("select_date_time")}
                  >
                    <Radio
                      name={"select_time"}
                      id={"iso_date_format"}
                      label={t("iso_date_format")}
                      isChecked={selectDateTimeFormat === "ISO"}
                      onChange={() => onDateTimeSelect("ISO")}
                    />
                    <Radio
                      name={"select_time"}
                      id={"unix_date_format"}
                      label={t("unix_date_format")}
                      isChecked={selectDateTimeFormat === "Epoch"}
                      onChange={() => onDateTimeSelect("Epoch")}
                    />
                  </FormGroup>
                  <FormGroup>
                    <TextInput
                      id="date-input"
                      name={"date-input"}
                      type={selectDateTimeFormat === "ISO" ? "text" : "number"}
                      placeholder={
                        selectDateTimeFormat === "ISO"
                          ? "yyyy-MM-dd'T'HH:mm:ss.SSS"
                          : "specify epoch timestamp"
                      }
                      onChange={(_event, value) => handleDateTimeChange(value)}
                    />
                    {error?.SpecificDateTimeNotValidError && (
                      <FormHelperText>
                        <HelperText>
                          <HelperTextItem
                            icon={<ExclamationCircleIcon />}
                            variant={"error"}
                          >
                            {error.SpecificDateTimeNotValidError}
                          </HelperTextItem>
                        </HelperText>
                      </FormHelperText>
                    )}
                  </FormGroup>
                </>
              )}
            </FormSection>
            <ActionGroup>
              <Button
                variant="primary"
                onClick={handleSave}
                isDisabled={isLoading || !isEnabled}
              >
                {t("reset")}
              </Button>
              <DryrunSelect
                openDryrun={openDryrun}
                cliCommand={cliCommand}
                isDisabled={!isEnabled}
              />
              <Button
                variant="link"
                onClick={closeResetOffset}
                isDisabled={isLoading}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </Form>
        </PanelMainBody>
      </PanelMain>
    </Panel>
  );
}
