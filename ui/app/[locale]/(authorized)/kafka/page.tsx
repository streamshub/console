import { getKafkaClusters } from "@/api/kafka/actions";
import { RedirectOnLoad } from "@/components/Navigation/RedirectOnLoad";
import { redirect } from "@/i18n/routing";

export default function Page({}) {
  return redirect("/");
}
