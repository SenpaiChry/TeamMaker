import type { ButtonHTMLAttributes, ReactNode } from 'react'

/**
 * Tasti dell'app, portati dai drawable Android.
 *
 * `primary` riproduce `buttons_main`: blu pieno, angoli da 11dp, testo bianco
 * helvetica black corsivo maiuscolo, e il blu più chiaro alla pressione.
 */

type Variant = 'primary' | 'confirm' | 'danger' | 'ghost'

const VARIANTS: Record<Variant, string> = {
  // button_main_default → main_blue, pressed → #3EB6D0
  primary: 'bg-brand-blue text-white active:bg-brand-blue-pressed hover:bg-brand-blue-pressed',
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
      className={`app-title rounded-[11px] px-5 py-3 text-lg transition
                  disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:brightness-100
                  ${VARIANTS[variant]} ${className}`}
    >
      {children}
    </button>
  )
}
