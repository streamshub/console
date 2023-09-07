import { getClusters } from "@/api/bookmarks";
import { CreateBookmarkStep1Page } from "./CreateBookmarkStep1Page.client";

export default async function AsyncCreateBookmarkStep1Page() {
  const clusters = await getClusters();
  return (
    <CreateBookmarkStep1Page
      clusters={clusters.map((c) => c.attributes.bootstrapServers)}
    />
  );
}
