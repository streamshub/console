import { ConnectButton } from "@/app/[locale]/kafka/[kafkaId]/@header/overview/ConnectButton";
import { AppHeader } from "@/components/AppHeader";

export default function OverviewHeader() {
  return (
    <AppHeader
      title={"Cluster overview"}
      subTitle={"lorem dolor ipsum"}
      actions={[<ConnectButton key={"cd"} />]}
    />
  );
}
