import { TableSkeleton } from "@/components/table";
import { PropsWithChildren, Suspense } from "react";

export default function Content({ children }: PropsWithChildren) {
  return (
    <Suspense fallback={<TableSkeleton columns={5} rows={3} />}>
      {children}
    </Suspense>
  );
}
