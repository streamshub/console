/**
 * Topic Configuration Tab - Shows configuration settings for a topic
 */

import { useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import {
  PageSection,
  EmptyState,
  EmptyStateBody,
  Title,
  Spinner,
  Button,
  FormGroup,
  TextInput,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from '@patternfly/react-core';
import { CheckIcon, PencilAltIcon, TimesIcon } from '@patternfly/react-icons';
import { useQueryClient } from '@tanstack/react-query';
import { useTopic } from '@/api/hooks/useTopics';
import { ConfigValue } from '@/api/types';
import { apiClient } from '@/api/client';
import { hasPrivilege } from '@/utils/privileges';
import {
  ConfigurationTable,
  formatConfigValue,
} from '@/components/common/ConfigurationTable';

export function TopicConfigurationTab() {
  const { t } = useTranslation();
  const { kafkaId, topicId } = useParams<{
    kafkaId: string;
    topicId: string;
  }>();
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useTopic(kafkaId, topicId, {
    fields: ['name', 'configs'],
    includeDocumentation: true,
  });

  const [isEditing, setIsEditing] = useState<
    Record<string, 'editing' | 'saving' | undefined>
  >({});
  const [editValues, setEditValues] = useState<Record<string, string>>({});
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const topic = data?.data;
  const hasUpdatePrivilege = hasPrivilege('UPDATE', topic);

  const clearEditState = (name: string) => {
    setIsEditing((prev) => ({ ...prev, [name]: undefined }));
    setEditValues((prev) => {
      const next = { ...prev };
      delete next[name];
      return next;
    });
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next[name];
      return next;
    });
  };

  const handleSaveProperty = async (name: string, value: string) => {
    if (!kafkaId || !topicId) return;

    setIsEditing((prev) => ({ ...prev, [name]: 'saving' }));

    try {
      await apiClient.patch(`/api/kafkas/${kafkaId}/topics/${topicId}`, {
        data: {
          type: 'topics',
          id: topicId,
          attributes: { configs: { [name]: { value } } },
        },
      });

      await queryClient.invalidateQueries({
        queryKey: ['topic', kafkaId, topicId],
      });

      clearEditState(name);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : t('common.error');
      setFieldErrors((prev) => ({ ...prev, [name]: errorMessage }));
      setIsEditing((prev) => ({ ...prev, [name]: 'editing' }));
    }
  };

  const renderValue = (name: string, property: ConfigValue) => {
    if (isEditing[name] === undefined) {
      return formatConfigValue(property);
    }

    const validated = fieldErrors[name] ? 'error' : 'default';

    return (
      <FormGroup fieldId={name}>
        <TextInput
          id={`property-${name}`}
          placeholder={property.value}
          value={editValues[name] ?? property.value}
          onChange={(_, value: string) =>
            setEditValues((prev) => ({ ...prev, [name]: value }))
          }
          validated={validated}
          isDisabled={isEditing[name] === 'saving'}
        />
        {validated === 'error' && (
          <FormHelperText>
            <HelperText>
              <HelperTextItem variant={validated}>
                {fieldErrors[name]}
              </HelperTextItem>
            </HelperText>
          </FormHelperText>
        )}
      </FormGroup>
    );
  };

  const renderActions = (name: string, property: ConfigValue) => {
    if (property.readOnly || !hasUpdatePrivilege) {
      return null;
    }

    if (isEditing[name] === undefined) {
      return (
        <Button
          icon={<PencilAltIcon />}
          variant="plain"
          onClick={() =>
            setIsEditing((prev) => ({ ...prev, [name]: 'editing' }))
          }
          aria-label={t('common.edit')}
        />
      );
    }

    const isSaving = isEditing[name] === 'saving';

    return (
      <div className="pf-v6-c-inline-edit pf-m-inline-editable">
        <div className="pf-v6-c-inline-edit__group pf-m-action-group pf-m-icon-group">
          <div className="pf-v6-c-inline-edit__action pf-m-valid">
            <Button
              icon={<CheckIcon />}
              variant="plain"
              isLoading={isSaving}
              isDisabled={isSaving}
              onClick={() =>
                handleSaveProperty(name, editValues[name] ?? property.value)
              }
              aria-label={t('common.save')}
            />
          </div>
          <div className="pf-v6-c-inline-edit__action">
            <Button
              icon={<TimesIcon />}
              variant="plain"
              isDisabled={isSaving}
              onClick={() => clearEditState(name)}
              aria-label={t('common.cancel')}
            />
          </div>
        </div>
      </div>
    );
  };

  if (isLoading) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Spinner size="xl" />
          <Title headingLevel="h2" size="lg">
            {t('common.loading')}
          </Title>
        </EmptyState>
      </PageSection>
    );
  }

  if (error) {
    return (
      <PageSection isFilled>
        <EmptyState>
          <Title headingLevel="h2" size="lg">
            {t('common.error')}
          </Title>
          <EmptyStateBody>{error.message}</EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <ConfigurationTable
      configs={topic?.attributes.configs}
      ariaLabel="Topic configuration table"
      ouiaId="topic-configuration-table"
      isFilled
      renderValue={renderValue}
      renderActions={renderActions}
      showDocumentation
    />
  );
}
