import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PLAYERS_PER_TEAM_OPTIONS } from '@/domain/constants'
import { countTeams, hasEnoughPlayers } from '@/domain/teamGenerator'

/**
 * Scelta dei giocatori per squadra.
 * Porta ActivityPopUpGenerateTeams, compreso il vincolo che servano almeno
 * due squadre (la seconda anche incompleta).
 */
export function GenerateOptionsModal({
  open,
  selectedCount,
  onClose,
  onConfirm,
}: {
  open: boolean
  selectedCount: number
  onClose: () => void
  onConfirm: (playersPerTeam: number) => void
}) {
  const [choice, setChoice] = useState<number | null>(null)
  const valid = choice !== null && hasEnoughPlayers(selectedCount, choice)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={<h2 className="text-lg font-bold tracking-wide">GIOCATORI PER SQUADRA</h2>}
    >
      <div className="grid grid-cols-4 gap-2">
        {PLAYERS_PER_TEAM_OPTIONS.map((n) => {
          const possible = hasEnoughPlayers(selectedCount, n)
          return (
            <button
              key={n}
              type="button"
              disabled={!possible}
              onClick={() => setChoice(n)}
              className={`rounded-lg border py-3 text-lg font-bold transition
                          disabled:cursor-not-allowed disabled:opacity-30 ${
                            choice === n
                              ? 'border-brand-blue bg-action-selected text-white'
                              : 'border-list-card-border bg-list-card'
                          }`}
            >
              {n}
            </button>
          )
        })}
      </div>

      <p className="mt-3 min-h-10 text-sm text-list-text-muted">
        {choice === null ? (
          `${selectedCount} giocatori selezionati.`
        ) : valid ? (
          <>
            {countTeams(selectedCount, choice)} squadre da {choice}
            {selectedCount % choice !== 0 && ', l’ultima incompleta'}.
          </>
        ) : (
          `Servono almeno ${choice * 2 - 1} giocatori per squadre da ${choice}.`
        )}
      </p>

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          ANNULLA
        </Button>
        <Button
          variant="confirm"
          disabled={!valid}
          onClick={() => choice !== null && onConfirm(choice)}
          className="grow"
        >
          GENERA
        </Button>
      </div>
    </Modal>
  )
}
