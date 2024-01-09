import { Flex, FlexItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";
import { truncate } from "./utils";

const PREVIEW_LENGTH = 170;

export type UnknownValuePreviewProps = {
  value: string;
  truncateAt?: number;
  onClick?: () => void;
};

export function UnknownValuePreview({
  value,
  truncateAt = PREVIEW_LENGTH,
  onClick,
}: UnknownValuePreviewProps) {
  const t = useTranslations("message-browser");
  const [preview, truncated] = truncate(value, truncateAt);
  return (
    <Flex
      direction={{ default: "column" }}
      spaceItems={{ default: "spaceItemsXs" }}
      onClick={
        onClick
          ? (e) => {
              e.stopPropagation();
              onClick();
            }
          : undefined
      }
    >
      <FlexItem>{preview}</FlexItem>
      {truncated && (
        <FlexItem>
          <a>{t("show_more")}</a>
        </FlexItem>
      )}
    </Flex>
  );
}
