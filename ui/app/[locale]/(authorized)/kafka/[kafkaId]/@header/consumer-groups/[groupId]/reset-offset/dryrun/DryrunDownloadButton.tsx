'use client'

import { Button } from '@/libs/patternfly/react-core'
import { DownloadIcon } from '@/libs/patternfly/react-icons'
import { useTranslations } from 'next-intl'
import { Offset } from '../../../../../consumer-groups/[groupId]/reset-offset/ResetConsumerOffset'

export function DryrunDownloadButton({
  groupId,
  offsets,
}: {
  groupId: string
  offsets: Offset[]
}) {
  const t = useTranslations('ConsumerGroupsTable')

  const onClickDownload = () => {
    const data = {
      groupId,
      offsets,
    }
    const jsonString = JSON.stringify(data, null, 2)
    const blob = new Blob([jsonString], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'dryrun-result.json'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  return (
    <Button
      ouiaId={'dryrun-download-button'}
      variant="link"
      onClick={onClickDownload}
    >
      {
        <>
          {t('download_dryrun_result')} <DownloadIcon />
        </>
      }
    </Button>
  )
}
