import { getClusters } from "@/api/bookmarks";
import { getSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { CreateBookmarkStep3Page } from "./CreateBookmarkStep3Page.client";

export default async function AsyncNewAuthProfilePage() {
  const session = await getSession();
  const newBookmark = session?.newBookmark;
  console.log("wtf", session, newBookmark);

  const { name, principal, boostrapServer } = newBookmark || {};
  if (!name || !principal || !boostrapServer) {
    redirect("/");
  }

  const clusters = await getClusters();
  const cluster = clusters.find(
    (c) => c.attributes.bootstrapServers === boostrapServer,
  );
  return (
    <CreateBookmarkStep3Page
      name={name}
      principal={principal}
      bootstrapServer={boostrapServer}
      cluster={cluster}
    />
  );
}
