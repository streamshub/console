/**
 * Rebalances Count Card Component
 * Displays counts for different rebalance statuses
 */

import { useTranslation } from 'react-i18next';
import {
  Card,
  CardBody,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  Flex,
  FlexItem,
  Divider,
  Bullseye,
} from '@patternfly/react-core';

interface RebalancesCountCardProps {
  totalRebalances: number;
  proposalReady: number;
  rebalancing: number;
  ready: number;
  stopped: number;
}

export function RebalancesCountCard({
  totalRebalances,
  proposalReady,
  rebalancing,
  ready,
  stopped,
}: RebalancesCountCardProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardBody>
        <DescriptionList>
          <Flex justifyContent={{ default: 'justifyContentSpaceEvenly' }}>
            <FlexItem>
              <DescriptionListGroup>
                <DescriptionListTerm>{t('rebalancing.totalRebalances')}</DescriptionListTerm>
                <DescriptionListDescription>
                  <Bullseye>{totalRebalances}</Bullseye>
                </DescriptionListDescription>
              </DescriptionListGroup>
            </FlexItem>
            <Divider orientation={{ default: 'vertical' }} />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t('rebalancing.statuses.proposalReady.label')}</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{proposalReady}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider orientation={{ default: 'vertical' }} />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t('rebalancing.statuses.rebalancing.label')}</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{rebalancing}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider orientation={{ default: 'vertical' }} />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>{t('rebalancing.statuses.ready.label')}</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Bullseye>{ready}</Bullseye>
                  </DescriptionListDescription>
                </DescriptionListGroup>
              </Bullseye>
            </FlexItem>
            <Divider orientation={{ default: 'vertical' }} />
            <FlexItem>
              <Bullseye>
                <DescriptionListGroup>
                  <DescriptionListTerm>
                    <Bullseye>{t('rebalancing.statuses.stopped.label')}</Bullseye>
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