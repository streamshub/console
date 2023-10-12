import { truncate } from "./utils";
import { Flex, FlexItem } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

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
    >
      <FlexItem>{preview}</FlexItem>
      {truncated && (
        <FlexItem>
          <a
            onClick={
              onClick
                ? (e) => {
                    e.stopPropagation();
                    onClick();
                  }
                : undefined
            }
          >
            {t("show_more")}
          </a>
        </FlexItem>
      )}
    </Flex>
  );
}
