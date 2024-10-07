import {
  Bullseye,
  Card,
  CardBody,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Divider,
  Flex,
  FlexItem,
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
          <Flex justifyContent={{ default: "justifyContentSpaceEvenly" }}>
            <FlexItem>
              <DescriptionListGroup>
                <DescriptionListTerm>
                  {t("total_rebalances")}
                </DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{TotalRebalancing}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </FlexItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {t("proposal_ready")}
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{proposalReady}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("rebalancing")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{rebalancing}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t("ready")}</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{ready}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider
              orientation={{
                default: "vertical",
              }}
            />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    {" "}
                    <Bullseye>{t("stopped")}</Bullseye>
                  </DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{stopped}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
          </Flex>
        </DescriptionList>
      </CardBody>
    </Card>
  );
}
