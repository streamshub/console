import { Flex, FlexItem, Skeleton } from "@/libs/patternfly/react-core";
import { getHeight, getPadding } from "./chartConsts";

export function ChartSkeletonLoader() {
  const height = getHeight(0);
  const padding = getPadding(0);
  return (
    <Flex direction={{ default: "column" }} data-chromatic="ignore">
      <FlexItem>
        <Skeleton
          height={`${height - padding.bottom}px`}
          screenreaderText={"Loading"}
        />
      </FlexItem>
      <FlexItem>
        <Skeleton height={`${padding.bottom / 2 - 12.5}px`} width="20%" />
      </FlexItem>
      <FlexItem>
        <Skeleton height={`${padding.bottom / 2 - 12.5}px`} width="40%" />
      </FlexItem>
    </Flex>
  );
}
