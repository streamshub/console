import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Modal,
  ModalVariant,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Form,
  FormGroup,
  TextInput,
  Button,
  Alert,
  AlertVariant,
} from '@patternfly/react-core';

interface KafkaLoginModalProps {
  isOpen: boolean;
  clusterName: string;
  authMethod?: string;
  onClose: () => void;
  onLogin: (username: string, password: string) => Promise<void>;
  error?: string;
}

export function KafkaLoginModal({
  isOpen,
  clusterName,
  authMethod,
  onClose,
  onLogin,
  error,
}: KafkaLoginModalProps) {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Clear fields when modal closes
  useEffect(() => {
    if (!isOpen) {
      setUsername('');
      setPassword('');
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!username || !password) {
      return;
    }

    setIsSubmitting(true);
    try {
      await onLogin(username, password);
      // On success, the parent component will handle navigation
      // and close the modal
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      onClose();
    }
  };

  // Determine the appropriate description key based on authentication method
  const getDescriptionKey = () => {
    switch (authMethod) {
      case 'oauth':
        return 'kafka.login.descriptionOAuth';
      case 'basic':
        return 'kafka.login.descriptionBasic';
      default:
        return 'kafka.login.description';
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen={isOpen}
      onClose={handleClose}
    >
      <ModalHeader
        title={t('kafka.login.title', 'Login to Kafka Cluster')}
      />
      <ModalBody>
        <Form onSubmit={handleSubmit}>
          {error && (
            <Alert
              variant={AlertVariant.danger}
              title={t('kafka.login.error', 'Authentication failed')}
              isInline
              style={{ marginBottom: '1rem' }}
            >
              {error}
            </Alert>
          )}

          <p style={{ marginBottom: '1rem' }}>
            {t(getDescriptionKey(), { clusterName })}
          </p>

          <FormGroup
            label={t('kafka.login.username', 'Username')}
            isRequired
            fieldId="username"
          >
            <TextInput
              isRequired
              type="text"
              id="username"
              name="username"
              value={username}
              onChange={(_event, value) => setUsername(value)}
              isDisabled={isSubmitting}
              autoComplete="username"
            />
          </FormGroup>

          <FormGroup
            label={t('kafka.login.password', 'Password')}
            isRequired
            fieldId="password"
          >
            <TextInput
              isRequired
              type="password"
              id="password"
              name="password"
              value={password}
              onChange={(_event, value) => setPassword(value)}
              isDisabled={isSubmitting}
              autoComplete="current-password"
            />
          </FormGroup>
        </Form>
      </ModalBody>
      <ModalFooter>
        <Button
          key="login"
          variant="primary"
          onClick={handleSubmit}
          isDisabled={!username || !password || isSubmitting}
          isLoading={isSubmitting}
        >
          {t('kafka.login.submit', 'Login')}
        </Button>
        <Button
          key="cancel"
          variant="link"
          onClick={handleClose}
          isDisabled={isSubmitting}
        >
          {t('common.cancel', 'Cancel')}
        </Button>
      </ModalFooter>
    </Modal>
  );
}