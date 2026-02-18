'use client'
import { Button, Spinner, Tooltip } from '@/libs/patternfly/react-core'
import { SyncAltIcon } from '@/libs/patternfly/react-icons'
import { useRouter } from '@/i18n/routing'
import { ButtonProps } from '@/libs/patternfly/react-core'
import { useTranslations } from 'next-intl'
import { useTransition } from 'react'

export type RefreshButtonProps = {
  tooltip?: string
  ariaLabel?: string
  onClick?: () => void
}

export function RefreshButton({
  ariaLabel,
  onClick,
  tooltip,
}: RefreshButtonProps) {
  const t = useTranslations()
  const router = useRouter()
  const [isRefreshing, startTransition] = useTransition()

  const handleClick: ButtonProps['onClick'] =
    onClick ??
    ((e) => {
      e.preventDefault()
      startTransition(() => {
        router.refresh()
      })
    })

  const defaultTooltip = isRefreshing
    ? t('RefreshButton.refreshing_tooltip')
    : t('RefreshButton.refresh_description')

  return (
    <Tooltip content={tooltip || defaultTooltip}>
      <Button
        ouiaId={'refresh-button'}
        className="pf-m-hoverable"
        variant="plain"
        aria-label={ariaLabel || t('RefreshButton.refresh_button_label')}
        isDisabled={isRefreshing}
        onClick={handleClick}
        icon={isRefreshing ? <Spinner size={'md'} /> : <SyncAltIcon />}
      />
    </Tooltip>
  )
}
