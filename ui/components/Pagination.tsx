'use client'
import type { PaginationProps as PFPaginationProps } from '@/libs/patternfly/react-core'
import {
  Pagination as PFPagination,
  PaginationVariant,
} from '@/libs/patternfly/react-core'
import { NoSSR } from './NoSSR'

export type PaginationProps = {
  itemCount: number
  page: number
  perPage: number
  isCompact?: boolean
  onChange: (page: number, perPage: number) => void
} & Pick<PFPaginationProps, 'variant' | 'ouiaId'>
export function Pagination({
  itemCount,
  page,
  perPage,
  isCompact = false,
  onChange,
  variant = PaginationVariant.top,
  ouiaId,
}: PaginationProps) {
  const variantSuffix = variant || 'top'
  const widgetId = `pagination-${variantSuffix}`
  const defaultOuiaId = `pagination-${variantSuffix}`
  return (
    <NoSSR>
      <PFPagination
        widgetId={widgetId}
        ouiaId={ouiaId || defaultOuiaId}
        id={widgetId}
        itemCount={itemCount}
        page={page}
        perPage={perPage}
        onSetPage={(_, page) => onChange(page, perPage)}
        onPerPageSelect={(_, perPage) => onChange(1, perPage)}
        variant={variant}
        isCompact={isCompact}
      />
    </NoSSR>
  )
}
