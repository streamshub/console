'use client'

import { Button, Tooltip } from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { useRouter } from 'next/navigation'

export function ConsumerGroupActionButton({
  disabled,
  kafkaId,
  groupId,
}: {
  disabled: boolean
  kafkaId: string
  groupId: string
}) {
  const t = useTranslations()
  const router = useRouter()

  const onClickResetOffset = () => {
    router.push(`/kafka/${kafkaId}/consumer-groups/${groupId}/reset-offset`)
  }

  return (
    <Tooltip
      key={'reset'}
      content={
        'It is possible to reset the offset only on stopped consumer groups'
      }
    >
      <Button
        ouiaId={'consumer-group-action-button'}
        isDisabled={disabled}
        aria-disabled={disabled}
        id={'reset'}
        onClick={onClickResetOffset}
      >
        {t('ConsumerGroup.reset_offset')}
      </Button>
    </Tooltip>
    // <Tooltip
    //   key={"delete"}
    //   content={"It is possible to delete only stopped consumer groups"}
    // >
    //   <Button
    //     variant={"danger"}
    //     aria-disabled={disabled}
    //     isDisabled={disabled}
    //   >
    //     {t("ConsumerGroup.delete")}
    //   </Button>
    // </Tooltip>,
  )
}
