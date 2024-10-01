import {
  Bullseye,
  Card,
  CardBody,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  Level,
  LevelItem,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function RebalancesCountCard({
  TotalRebalancing,
  proposalReady,
  rebalancing,
  ready,
  stopped,
}: {
  TotalRebalancing: number;
  proposalReady: number;
  rebalancing: number;
  ready: number;
  stopped: number;
}) {
  const t = useTranslations("Rebalancing");
  return (
    <Card>
      <CardBody>
        <DescriptionList>
          <Level hasGutter>
            <LevelItem>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("total_rebalances")}
                </DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{TotalRebalancing}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </LevelItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <LevelItem>
              <DescriptionListGroup>
                <DescriptionListTerm>{t("proposal_ready")}</DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{proposalReady}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </LevelItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <LevelItem>
              <DescriptionListGroup>
                <DescriptionListTerm>{t("rebalancing")}</DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{rebalancing}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </LevelItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <LevelItem>
              <DescriptionListGroup>
                <DescriptionListTerm>{t("ready")}</DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{ready}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </LevelItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <LevelItem>
              <DescriptionListGroup>
                <DescriptionListTerm>{t("stopped")}</DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{stopped}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </LevelItem>
          </Level>
        </DescriptionList>
      </CardBody>
    </Card>
  );
}
