import { Message } from "@/api/messages/schema";
import { DateTime } from "@/components/DateTime";
import { Number } from "@/components/Number";
import {
  ClipboardCopy,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  DrawerActions,
  DrawerCloseButton,
  DrawerContentBody,
  DrawerHead,
  DrawerPanelContent,
  Flex,
  FlexItem,
  Tab,
  Tabs,
  TabTitleText,
  Text,
  TextContent,
  TextVariants,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { NoDataCell } from "./NoDataCell";
import { beautifyUnknownValue } from "./utils";

export type MessageDetailsProps = {
  onClose: () => void;
  defaultTab: MessageDetailsBodyProps["defaultTab"];
  message: Message | undefined;
};

export function MessageDetails({
  onClose,
  defaultTab,
  message,
}: MessageDetailsProps) {
  const t = useTranslations("message-browser");

  return (
    <DrawerPanelContent isResizable={true} minSize={"400px"}>
      <DrawerHead>
        <TextContent>
          <Text component={TextVariants.h2}>{t("message")}</Text>
        </TextContent>
        <DrawerActions>
          <DrawerCloseButton onClick={onClose} />
        </DrawerActions>
      </DrawerHead>
      <DrawerContentBody>
        {message && (
          <MessageDetailsBody
            defaultTab={defaultTab}
            messageKey={message.attributes.key}
            {...message}
          />
        )}
      </DrawerContentBody>
    </DrawerPanelContent>
  );
}

export type MessageDetailsBodyProps = {
  defaultTab: "value" | "key" | "headers";
  messageKey: string | null;
} & Omit<Message, "key">;

export function MessageDetailsBody({
  defaultTab,
  ...message
}: MessageDetailsBodyProps) {
  const t = useTranslations("message-browser");

  return (
    <Flex direction={{ default: "column" }} data-testid={"message-details"}>
      <FlexItem>
        <DescriptionList isHorizontal isCompact>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("field.partition")}</DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={message.attributes.partition} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("field.offset")}</DescriptionListTerm>
            <DescriptionListDescription>
              <Number value={message.attributes.offset} />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("field.timestamp")}</DescriptionListTerm>
            <DescriptionListDescription>
              <DateTime
                value={message.attributes.timestamp}
                empty={<NoDataCell columnLabel={t("field.timestamp")} />}
              />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>
              {t("field.timestamp--utc")}
            </DescriptionListTerm>
            <DescriptionListDescription>
              <DateTime
                value={message.attributes.timestamp}
                empty={<NoDataCell columnLabel={t("field.timestamp")} />}
                tz={"UTC"}
              />
            </DescriptionListDescription>
          </DescriptionListGroup>
          <DescriptionListGroup>
            <DescriptionListTerm>{t("field.epoch")}</DescriptionListTerm>
            <DescriptionListDescription>
              {message.attributes.timestamp ? (
                Math.floor(
                  new Date(message.attributes.timestamp).getTime() / 1000,
                )
              ) : (
                <NoDataCell columnLabel={t("field.epoch")} />
              )}
            </DescriptionListDescription>
          </DescriptionListGroup>
        </DescriptionList>
      </FlexItem>
      <FlexItem>
        {/* set key to be a random number to force redraw the tabs in order to change the active tab from the outside */}
        <Tabs defaultActiveKey={defaultTab} key={Math.random()}>
          <Tab
            eventKey={"value"}
            title={<TabTitleText>{t("field.value")}</TabTitleText>}
          >
            <ClipboardCopy isCode={true} isExpanded={true} isReadOnly={true}>
              {beautifyUnknownValue(
                message.attributes.value || "Message has no value",
              )}
            </ClipboardCopy>
          </Tab>
          <Tab
            eventKey={"key"}
            title={<TabTitleText>{t("field.key")}</TabTitleText>}
          >
            <ClipboardCopy isCode={true} isExpanded={true} isReadOnly={true}>
              {beautifyUnknownValue(
                message.attributes.key || "Message has no key",
              )}
            </ClipboardCopy>
          </Tab>
          <Tab
            eventKey={"headers"}
            title={<TabTitleText>{t("field.headers")}</TabTitleText>}
          >
            <ClipboardCopy isCode={true} isExpanded={true} isReadOnly={true}>
              {beautifyUnknownValue(
                JSON.stringify(message.attributes.headers) ||
                  "Message has no header",
              )}
            </ClipboardCopy>
          </Tab>
        </Tabs>
      </FlexItem>
    </Flex>
  );
}
