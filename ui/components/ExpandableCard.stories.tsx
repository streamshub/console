import type { Meta, StoryObj } from '@storybook/nextjs'
import { ExpandableCard } from './ExpandableCard'

const meta: Meta<typeof ExpandableCard> = {
  component: ExpandableCard,
}

export default meta
type Story = StoryObj<typeof ExpandableCard>

export const Default: Story = {}
