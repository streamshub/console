import { getKafkaClusters } from "@/api/kafka";
import { redirect } from "@/navigation";
import { getSession } from "@/utils/session";
import { Step3 } from "./Step3";

export default async function AsyncNewAuthProfilePage() {
  const session = await getSession("resources");
  const newResource = session?.newResource;

  const { name, principal, boostrapServer } = newResource || {};
  if (!name || !principal || !boostrapServer) {
    redirect("/");
    return null;
  }

  const clusters = await getKafkaClusters();
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
