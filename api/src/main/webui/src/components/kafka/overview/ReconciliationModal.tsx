import { useTranslation } from 'react-i18next';
import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
} from '@patternfly/react-core';

interface ReconciliationModalProps {
  isOpen: boolean;
  isReconciliationPaused: boolean;
  isPending?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function ReconciliationModal({
  isOpen,
  isReconciliationPaused,
  isPending = false,
  onClose,
  onConfirm,
}: ReconciliationModalProps) {
  const { t } = useTranslation();

  return (
    <Modal
      isOpen={isOpen}
      variant={ModalVariant.medium}
      onClose={onClose}
    >
      <ModalHeader
        title={
          isReconciliationPaused
            ? t('reconciliation.resume_cluster_reconciliation')
            : t('reconciliation.pause_reconciliation')
        }
      />
      <ModalBody>
        {isReconciliationPaused
          ? t('reconciliation.resume_cluster_reconciliation_description')
          : t('reconciliation.pause_reconciliation_text')}
      </ModalBody>
      <ModalFooter>
        <Button
          key="confirm"
          variant="primary"
          onClick={onConfirm}
          isDisabled={isPending}
        >
          {t('reconciliation.confirm')}
        </Button>
        <Button
          key="cancel"
          variant="link"
          onClick={onClose}
          isDisabled={isPending}
        >
          {t('reconciliation.cancel')}
        </Button>
      </ModalFooter>
    </Modal>
  );
}