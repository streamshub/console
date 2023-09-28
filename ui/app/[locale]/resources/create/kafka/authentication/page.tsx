import { getSession } from "@/utils/session";
import { redirect } from "next/navigation";
import { Step2 } from "./Step2";

export default async function AsyncCreateResourceStep2Page() {
  const session = await getSession("resources");
  const newResource = session?.newResource;
  const { name, boostrapServer } = newResource || {};
  if (!name || !boostrapServer) {
    redirect("/");
  }
  return <Step2 />;
}
