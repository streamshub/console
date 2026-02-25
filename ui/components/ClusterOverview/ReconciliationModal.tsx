'use client'

import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
} from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'

export function ReconciliationModal({
  isModalOpen,
  onClickClose,
  onClickPauseReconciliation,
  isReconciliationPaused,
}: {
  isModalOpen: boolean
  onClickClose: () => void
  onClickPauseReconciliation: () => void
  isReconciliationPaused: boolean
}) {
  const t = useTranslations()
  return (
    <Modal
      isOpen={isModalOpen}
      variant={ModalVariant.medium}
      onClose={onClickClose}
    >
      <ModalHeader
        title={
          isReconciliationPaused
            ? t('reconciliation.resume_cluster_reconciliation')
            : t('reconciliation.pause_reconciliation')
        }
      ></ModalHeader>
      <ModalBody>
        {isReconciliationPaused
          ? t('reconciliation.resume_cluster_reconciliation_description')
          : t('reconciliation.pause_reconciliation_text')}
      </ModalBody>

      <ModalFooter>
        <Button
          ouiaId={'reconciliation-confirm-button'}
          key="confirm"
          variant="primary"
          onClick={onClickPauseReconciliation}
        >
          {t('reconciliation.confirm')}
        </Button>
        <Button
          ouiaId={'reconciliation-cancel-button'}
          key="cancel"
          variant="link"
          onClick={onClickClose}
        >
          {t('reconciliation.cancel')}
        </Button>
      </ModalFooter>
    </Modal>
  )
}
