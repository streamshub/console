import { getClusters } from "@/api/resources";
import { getSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { Step3 } from "./Step3";

export default async function AsyncNewAuthProfilePage() {
  const session = await getSession("resources");
  const newResource = session?.newResource;
  console.log("wtf", session, newResource);

  const { name, principal, boostrapServer } = newResource || {};
  if (!name || !principal || !boostrapServer) {
    redirect("/");
  }

  const clusters = await getClusters();
  const cluster = clusters.find(
    (c) => c.attributes.bootstrapServers === boostrapServer,
  );
  return (
    <Step3
      name={name}
      principal={principal}
      bootstrapServer={boostrapServer}
      cluster={cluster}
    />
  );
}
