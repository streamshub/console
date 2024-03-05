import { Flex } from "@/libs/patternfly/react-core";
import truncate from "@stdlib/string-truncate";
import Highlighter from "react-highlight-words";

export type UnknownValuePreviewProps = {
  value: string;
  highlight?: string;
  onClick?: () => void;
};

export function UnknownValuePreview({
  value,
  highlight,
  onClick,
}: UnknownValuePreviewProps) {
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
      {highlight && value.includes(highlight) ? (
        <Highlighter
          searchWords={[highlight]}
          autoEscape={true}
          textToHighlight={value}
        />
      ) : (
        <TruncatedValue>{value}</TruncatedValue>
      )}
    </Flex>
  );
}

function TruncatedValue({ children }: { children: string }) {
  return truncate(children, 50000);
}
