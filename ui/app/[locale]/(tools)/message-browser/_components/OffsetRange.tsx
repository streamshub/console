import { Text, TextContent } from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function OffsetRange({ min, max }: { min: number; max: number }) {
  const t = useTranslations("message-browser");
  return (
    <TextContent className="pf-u-font-size">
      <Text>
        {t("offset")} <span className="custom-text">{min}</span> -{" "}
        <span className="custom-text">{max}</span>
      </Text>
    </TextContent>
  );
}
