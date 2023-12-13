import { Flex, FlexItem, Skeleton } from "@/libs/patternfly/react-core";

export function ChartSkeletonLoader({
  height,
  padding,
}: {
  height: number;
  padding: number;
}) {
  return (
    <Flex direction={{ default: "column" }} data-chromatic="ignore">
      <FlexItem>
        <Skeleton
          height={`${height - padding}px`}
          screenreaderText={"Loading"}
        />
      </FlexItem>
      <FlexItem>
        <Skeleton height={`${padding / 2 - 12.5}px`} width="20%" />
      </FlexItem>
      <FlexItem>
        <Skeleton height={`${padding / 2 - 12.5}px`} width="40%" />
      </FlexItem>
    </Flex>
  );
}
