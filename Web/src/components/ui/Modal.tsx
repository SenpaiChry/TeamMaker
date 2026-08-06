import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Modale, l'equivalente delle Activity con tema `NoTitleDialog` dell'app Android.
 * Usa <dialog> nativo, così Esc e il focus trap funzionano senza codice.
 */
export function Modal({
  open,
  onClose,
  title,
  children,
}: {
  open: boolean
  onClose: () => void
  title?: ReactNode
  children: ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (dialog === null) return

    if (open && !dialog.open) dialog.showModal()
    else if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => {
        // Clic sullo sfondo (il <dialog> stesso, non il contenuto) → chiudi.
        if (e.target === ref.current) onClose()
      }}
      className="m-auto w-[92vw] max-w-md rounded-xl border border-surface-modal-border
                 bg-score-bg-top p-0 text-list-text backdrop:bg-black/60"
    >
      <div className="p-5">
        {title !== undefined && <div className="mb-4">{title}</div>}
        {children}
      </div>
    </dialog>
  )
}
