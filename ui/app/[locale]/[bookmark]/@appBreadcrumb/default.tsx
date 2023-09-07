import { getBookmark } from "@/api/bookmarks";
import { AppBreadcrumbs } from "@/components/appBreadcrumbs";
import { Truncate } from "@/libs/patternfly/react-core";

export default async function ToolsBreadcrumb({
  params,
}: {
  params: {
    bookmark: string;
  };
}) {
  const bookmark = await getBookmark(params.bookmark);
  return (
    <AppBreadcrumbs
      bookmarkName={
        <span style={{ display: "inline-block", maxWidth: "max(50vw, 300px)" }}>
          {bookmark.attributes.name} ({bookmark.attributes.principal}@
          <Truncate
            content={bookmark.attributes.bootstrapServer}
            position={"middle"}
          />
          )
        </span>
      }
    />
  );
}
