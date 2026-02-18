import {
  ExpandableSection as PFExpandableSection,
  ExpandableSectionProps,
} from '@/libs/patternfly/react-core'
import { useState } from 'react'

export function ExpandableSection({
  initialExpanded = true,
  ...props
}: Omit<ExpandableSectionProps, 'isExpanded' | 'onToggle' | 'ref'> & {
  initialExpanded?: boolean
}) {
  const [expanded, setExpanded] = useState(initialExpanded)

  return (
    <PFExpandableSection
      {...props}
      isExpanded={expanded}
      onToggle={(_, e) => setExpanded(e)}
    />
  )
}
