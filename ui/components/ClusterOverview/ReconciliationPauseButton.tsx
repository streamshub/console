'use client'

import { updateKafkaCluster } from '@/api/kafka/actions'
import { ReconciliationModal } from '@/components/ClusterOverview/ReconciliationModal'
import { useReconciliationContext } from '@/components/ReconciliationContext'
import { Button } from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { useState } from 'react'
import { PauseCircleIcon, PlayIcon } from '@/libs/patternfly/react-icons'

export function ReconciliationPauseButton({
  clusterId,
  managed,
}: {
  clusterId: string
  managed: boolean
}) {
  const t = useTranslations()
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false)

  const {
    isReconciliationPaused,
    setReconciliationPaused,
  } = useReconciliationContext()

  const onClickUpdate = async (pausedState: boolean) => {
    try {
      const response = await updateKafkaCluster(clusterId, pausedState)

      if (response.errors) {
        console.log('Unknown error occurred', response.errors)
      } else {
        setReconciliationPaused(pausedState)
        setIsModalOpen(false)
      }
    } catch (e) {
      console.log('Unknown error occurred')
    }
  }

  return (
    <>
      {managed && (
        <Button
          ouiaId={'reconciliation-button'}
          variant="link"
          icon={isReconciliationPaused ? <PlayIcon /> : <PauseCircleIcon />}
          onClick={() => setIsModalOpen(true)}
        >
          {isReconciliationPaused
            ? t('reconciliation.resume_reconciliation')
            : t('reconciliation.pause_reconciliation_button')}
        </Button>
      )}
      {isModalOpen && (
        <ReconciliationModal
          isModalOpen={isModalOpen}
          onClickClose={() => setIsModalOpen(false)}
          onClickPauseReconciliation={
            isReconciliationPaused
              ? () => onClickUpdate(false)
              : () => onClickUpdate(true)
          }
          isReconciliationPaused={isReconciliationPaused}
        />
      )}
    </>
  )
}
