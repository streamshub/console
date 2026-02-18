'use client'

import { ApiResponse, ApiError } from '@/api/api'
import {
  ConfigMap,
  NewConfigMap,
  TopicCreateResponse,
} from '@/api/topics/schema'
import { StepDetails } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/StepDetails'
import { StepOptions } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/StepOptions'
import { StepReview } from '@/app/[locale]/(authorized)/kafka/[kafkaId]/topics/create/StepReview'
import { useRouter } from '@/i18n/routing'
import {
  Button,
  PageSection,
  Tooltip,
  useWizardContext,
  Wizard,
  WizardFooterWrapper,
  WizardStep,
} from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { useCallback, useState, useTransition } from 'react'

export function CreateTopic({
  kafkaId,
  maxReplicas,
  initialOptions,
  onSave,
}: {
  kafkaId: string
  maxReplicas: number
  initialOptions: ConfigMap
  onSave: (
    name: string,
    partitions: number,
    replicas: number,
    options: NewConfigMap,
    validateOnly: boolean,
  ) => Promise<ApiResponse<TopicCreateResponse>>
}) {
  const t = useTranslations()
  const router = useRouter()
  const [name, setName] = useState('')
  const [partitions, setPartitions] = useState(1)
  const [replicas, setReplicas] = useState(maxReplicas)
  const [options, setOptions] = useState<NewConfigMap>({})
  const [pending, startTransition] = useTransition()
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState<ApiError[] | undefined>(undefined)

  const save = useCallback(async () => {
    try {
      setLoading(true)
      setErrors(undefined)
      const response = await onSave(name, partitions, replicas, options, false)

      startTransition(() => {
        if (response.errors) {
          setErrors(response.errors)
        } else {
          router.push(`/kafka/${kafkaId}/topics/${response.payload?.data.id}`)
        }
      })
    } catch (e) {
      setErrors([
        {
          title: 'Unknown error',
          detail: String(e),
        },
      ])
    } finally {
      setLoading(false)
    }
  }, [kafkaId, name, onSave, options, partitions, replicas, router])

  const validate = useCallback(
    async (success: () => void) => {
      try {
        setLoading(true)
        setErrors(undefined)
        const response = await onSave(name, partitions, replicas, options, true)
        startTransition(() => {
          if (response.errors) {
            setErrors(response.errors)
          } else {
            success()
          }
        })
      } catch (e) {
        setErrors([
          {
            title: 'Unknown error',
            detail: String(e),
          },
        ])
      } finally {
        setLoading(false)
      }
    },
    [name, onSave, options, partitions, replicas],
  )

  const formInvalid = errors !== undefined

  return (
    <Wizard navAriaLabel={t('CreateTopic.title')} onClose={() => router.back()}>
      <WizardStep
        name={t('CreateTopic.topic_details')}
        id="step-details"
        footer={
          <SkipReviewFooter
            formInvalid={formInvalid}
            onClick={(success) => validate(success)}
            loading={pending || loading}
          />
        }
      >
        <StepDetails
          name={name}
          partitions={partitions}
          replicas={replicas}
          maxReplicas={maxReplicas}
          onNameChange={setName}
          onPartitionsChange={setPartitions}
          onReplicasChange={setReplicas}
          errors={errors}
        />
      </WizardStep>
      <WizardStep
        name={t('CreateTopic.options')}
        id="step-options"
        footer={
          <AsyncFooter
            nextStepId={'step-review'}
            nextDisabled={formInvalid || errors !== undefined}
            onClick={(success) => validate(success)}
            loading={pending || loading}
            primaryLabel={t('CreateTopic.next')}
          />
        }
      >
        <StepOptions
          options={options}
          initialOptions={initialOptions}
          onChange={setOptions}
          errors={errors}
        />
      </WizardStep>
      <WizardStep
        name={t('CreateTopic.review')}
        id="step-review"
        footer={
          <AsyncFooter
            nextStepId={''}
            nextDisabled={formInvalid}
            onClick={save}
            loading={pending || loading}
            primaryLabel={t('CreateTopic.create_topic')}
          />
        }
      >
        <StepReview
          name={name}
          partitions={partitions}
          replicas={replicas}
          options={options}
          initialOptions={initialOptions}
          errors={errors}
        />
      </WizardStep>
    </Wizard>
  )
}

const SkipReviewFooter = ({
  formInvalid,
  onClick,
  loading,
}: {
  formInvalid: boolean
  onClick: (success: () => void) => void
  loading: boolean
}) => {
  const t = useTranslations()
  const { goToNextStep, goToStepById, close } = useWizardContext()
  return (
    <WizardFooterWrapper>
      <Button ouiaId={'create-topic-back-button'} isDisabled={true}>
        {t('CreateTopic.back')}
      </Button>
      <Button
        variant="primary"
        onClick={() => onClick(goToNextStep)}
        isLoading={loading}
        isDisabled={loading}
        ouiaId={'create-topic-next-button'}
      >
        {t('CreateTopic.next')}
      </Button>
      <Tooltip
        content={t('CreateTopic.review_and_finish_tooltip')}
        triggerRef={() => document.getElementById('review-button')!}
      >
        <Button
          ouiaId={'create-topic-review-button'}
          variant="plain"
          onClick={() => onClick(() => goToStepById('step-review'))}
          id={'review-button'}
          isDisabled={loading}
        >
          {t('CreateTopic.review_and_finish')}
        </Button>
      </Tooltip>
      <Button
        ouiaId={'create-topic-cancel-button'}
        variant={'link'}
        onClick={close}
      >
        {t('CreateTopic.cancel')}
      </Button>
    </WizardFooterWrapper>
  )
}

const AsyncFooter = ({
  nextStepId,
  nextDisabled,
  loading,
  primaryLabel,
  onClick,
}: {
  nextStepId: string
  nextDisabled: boolean
  loading: boolean
  primaryLabel: string
  onClick: (success: () => void) => void
}) => {
  const t = useTranslations()
  const { goToPrevStep, goToStepById, close } = useWizardContext()
  return (
    <WizardFooterWrapper>
      <Button
        ouiaId={'create-topic-back-button'}
        variant={'secondary'}
        onClick={goToPrevStep}
        disabled={loading}
      >
        {t('CreateTopic.back')}
      </Button>
      <Button
        ouiaId={'create-topic-next-button'}
        variant="primary"
        onClick={() =>
          onClick(() => {
            goToStepById(nextStepId)
          })
        }
        isLoading={loading}
        disabled={nextDisabled || loading}
      >
        {primaryLabel}
      </Button>
      <Button
        ouiaId={'create-topic-cancel-button'}
        variant={'link'}
        onClick={close}
        disabled={loading}
      >
        {t('CreateTopic.cancel')}
      </Button>
    </WizardFooterWrapper>
  )
}
