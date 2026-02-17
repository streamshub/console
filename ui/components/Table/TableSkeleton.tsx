import { Skeleton } from "@/libs/patternfly/react-core";
import { Td, Tr, Tbody } from "@/libs/patternfly/react-table";
import { useTranslations } from "next-intl";

type Props = {
  columns: number;
  rows: number;
  getTd?: (index: number) => typeof Td;
};
export function TableSkeleton({ columns, rows }: Props) {
  const t = useTranslations();
  const skeletonCells = new Array(columns).fill(0).map((_, index) => {
    return (
      <Td key={`cell_${index}`}>
        <Skeleton
          screenreaderText={
            index === 0
              ? t("Table.skeleton_loader_screenreader_text")
              : undefined
          }
        />
      </Td>
    );
  });
  const skeletonRows = new Array(rows)
    .fill(0)
    .map((_, index) => <Tr key={`row_${index}`}>{skeletonCells}</Tr>);
  return <Tbody>{skeletonRows}</Tbody>;
}
