import { getKafkaClusters } from "@/api/kafka/actions";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";
import { redirect } from "@/navigation";

export default function Page({}) {
  return redirect("/");
}
