import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'confirm' | 'danger' | 'ghost'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand-blue text-black hover:brightness-110',
  confirm: 'bg-action-confirm text-white hover:brightness-110',
  danger: 'bg-action-danger text-white hover:brightness-110',
  ghost: 'border border-list-card-border bg-list-card text-list-text hover:bg-score-panel',
}

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  children: ReactNode
}

export function Button({ variant = 'primary', className = '', children, ...props }: Props) {
  return (
    <button
      {...props}
      className={`rounded-lg px-5 py-3 font-bold tracking-wide transition
                  disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:brightness-100
                  ${VARIANTS[variant]} ${className}`}
    >
      {children}
    </button>
  )
}
