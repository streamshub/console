/**
 * Rebalance Confirmation Modal Component
 * Displays a confirmation dialog before approving, stopping, or refreshing a rebalance
 */

import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  Button,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from '@patternfly/react-core';

interface RebalanceConfirmationModalProps {
  isOpen: boolean;
  action: 'approve' | 'stop' | 'refresh';
  onConfirm: () => void;
  onCancel: () => void;
}

export function RebalanceConfirmationModal({
  isOpen,
  action,
  onConfirm,
  onCancel,
}: RebalanceConfirmationModalProps) {
  const { t } = useTranslation();

  const getTitleKey = () => {
    switch (action) {
      case 'approve':
        return 'rebalancing.confirmApproveTitle';
      case 'stop':
        return 'rebalancing.confirmStopTitle';
      case 'refresh':
        return 'rebalancing.confirmRefreshTitle';
    }
  };

  const getDescriptionKey = () => {
    switch (action) {
      case 'approve':
        return 'rebalancing.confirmApproveDescription';
      case 'stop':
        return 'rebalancing.confirmStopDescription';
      case 'refresh':
        return 'rebalancing.confirmRefreshDescription';
    }
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen={isOpen}
      onClose={onCancel}
      aria-label={t(getTitleKey())}
    >
      <ModalHeader title={t(getTitleKey())} />
      <ModalBody>{t(getDescriptionKey())}</ModalBody>
      <ModalFooter>
        <Button key="confirm" variant="primary" onClick={onConfirm}>
          {t('rebalancing.confirm')}
        </Button>
        <Button key="cancel" variant="link" onClick={onCancel}>
          {t('common.cancel')}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
