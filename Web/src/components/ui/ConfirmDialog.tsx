import { Modal } from './Modal'
import { Button } from './Button'

/** Conferma per le azioni che non si possono annullare. Porta ActivityPopUp. */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'CONFERMA',
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  message?: string
  confirmLabel?: string
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <Modal
      open={open}
      onClose={onCancel}
      title={<h2 className="text-lg font-bold tracking-wide">{title}</h2>}
    >
      {message !== undefined && <p className="text-list-text-secondary">{message}</p>}

      <div className="mt-5 flex gap-2">
        <Button variant="ghost" onClick={onCancel} className="grow">
          ANNULLA
        </Button>
        <Button variant="danger" onClick={onConfirm} className="grow">
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
