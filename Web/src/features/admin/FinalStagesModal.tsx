import { useMemo, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Tournament } from '@/domain/models'
import {
  availableFinalSizes,
  FINAL_SIZE,
  type FinalStageMatch,
  generateFinalStages,
  isPendingRef,
  pendingRefIndex,
  QUARTER_SIZE,
  SEMI_SIZE,
  SOURCE_GROUP_STANDING,
  SOURCE_LOSER,
  SOURCE_STANDING,
  SOURCE_WINNER,
} from '@/domain/finalStages'
import { FINAL, label as phaseLabel, QUARTER, SEMIFINAL } from '@/domain/phases'
import { addFinalStageMatches, resolveFinalStages } from '@/data/matchesRepo'

/**
 * Genera le fasi finali di un torneo — porta ActivityPopUpGenerateFinals.
 *
 * L'utente sceglie la dimensione del tabellone (fra quelle possibili per
 * numero squadre e gironi), decide se giocare la finale 3°/4° posto e vede
 * un'anteprima con i placeholder degli slot. Il salvataggio scrive tutte le
 * partite in una singola operazione e poi risolve subito quelle già risolvibili
 * (le posizioni di classifica, se la fase iniziale è già finita).
 *
 * Diversamente dalla generazione del calendario, qui non si assegnano giornata
 * e orario: le finali vengono inserite senza orario (day=0, time="0:00") e si
 * modificano una a una dall'editor partita, come nell'app Android.
 */
export function FinalStagesModal({
  tournament,
  open,
  onClose,
}: {
  tournament: Tournament
  open: boolean
  onClose: () => void
}) {
  const sizes = useMemo(() => availableFinalSizes(tournament), [tournament])
  const [size, setSize] = useState<number | null>(null)
  const [thirdPlace, setThirdPlace] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Se le sizes cambiano (raro, ma es. si torna alla modale dopo aggiungere
  // squadre), reimpostiamo la selezione sulla prima disponibile.
  const effectiveSize = size !== null && sizes.includes(size) ? size : (sizes[0] ?? null)

  const preview = useMemo(() => {
    if (effectiveSize === null) return []
    return generateFinalStages(tournament, effectiveSize, thirdPlace)
  }, [tournament, effectiveSize, thirdPlace])

  const submit = async () => {
    if (effectiveSize === null || preview.length === 0) return
    setBusy(true)
    setError(null)
    try {
      await addFinalStageMatches(tournament.key, preview)
      // Se la fase iniziale è già completa, i placeholder STANDING vanno
      // risolti subito. Ricarico il torneo dopo la scrittura non è banale
      // qui (i tornei arrivano via listener) — il resolver lavora comunque
      // sui dati attuali, che sono già in memoria: la scrittura appena fatta
      // non incluide risultati, quindi i WINNER/LOSER restano NULL, ma le
      // posizioni di classifica sono già risolvibili.
      await resolveFinalStages(tournament)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Generazione non riuscita.')
    } finally {
      setBusy(false)
    }
  }

  if (sizes.length === 0) {
    return (
      <Modal
        open={open}
        onClose={onClose}
        title={<h2 className="app-title text-lg">GENERA FINALI</h2>}
      >
        <p className="text-sm text-list-text-secondary">
          Non ci sono abbastanza squadre per una fase a eliminazione diretta.
        </p>
        <div className="mt-4 flex gap-2">
          <Button variant="ghost" onClick={onClose} className="grow">
            CHIUDI
          </Button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={<h2 className="app-title text-lg">GENERA FINALI</h2>}
    >
      <div className="grid grid-cols-3 gap-1 rounded-lg border border-list-card-border bg-list-card p-1">
        {sizes.includes(QUARTER_SIZE) ? (
          <SegmentButton
            active={effectiveSize === QUARTER_SIZE}
            onClick={() => setSize(QUARTER_SIZE)}
          >
            QUARTI
          </SegmentButton>
        ) : (
          <SegmentButton active={false} onClick={() => {}} disabled>
            QUARTI
          </SegmentButton>
        )}
        {sizes.includes(SEMI_SIZE) ? (
          <SegmentButton active={effectiveSize === SEMI_SIZE} onClick={() => setSize(SEMI_SIZE)}>
            SEMIFINALI
          </SegmentButton>
        ) : (
          <SegmentButton active={false} onClick={() => {}} disabled>
            SEMIFINALI
          </SegmentButton>
        )}
        {sizes.includes(FINAL_SIZE) ? (
          <SegmentButton active={effectiveSize === FINAL_SIZE} onClick={() => setSize(FINAL_SIZE)}>
            FINALE
          </SegmentButton>
        ) : (
          <SegmentButton active={false} onClick={() => {}} disabled>
            FINALE
          </SegmentButton>
        )}
      </div>

      <label className="mt-3 flex items-center gap-2">
        <input
          type="checkbox"
          checked={thirdPlace}
          onChange={(e) => setThirdPlace(e.target.checked)}
          className="size-4"
        />
        <span className="text-sm">Aggiungi finale 3°/4° posto</span>
      </label>

      {preview.length > 0 && (
        <div className="mt-4">
          <h3 className="app-title mb-2 text-xs text-list-text-muted">ANTEPRIMA</h3>
          <ul className="flex flex-col gap-1.5 rounded-[10px] border border-list-card-border bg-list-card p-2">
            {preview.map((match, i) => (
              <li
                key={i}
                className="flex items-center gap-2 rounded-md border border-list-card-border bg-score-panel px-2 py-1.5 text-[13px]"
              >
                <span className="app-title w-[26px] shrink-0 text-list-text-muted">
                  {shortPhase(match.type)}
                </span>
                <span className="min-w-0 grow truncate text-list-text">
                  {previewSlot(preview, i, 1)}
                </span>
                <span className="shrink-0 text-list-text-muted">vs</span>
                <span className="min-w-0 grow truncate text-right text-list-text">
                  {previewSlot(preview, i, 2)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {error !== null && <p className="mt-3 text-sm text-action-danger">{error}</p>}

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          ANNULLA
        </Button>
        <Button
          variant="confirm"
          onClick={submit}
          disabled={effectiveSize === null || busy}
          className="grow"
        >
          {busy ? 'GENERAZIONE…' : 'GENERA'}
        </Button>
      </div>
    </Modal>
  )
}

/**
 * Etichetta compatta di una fase per l'anteprima: "QUAR", "SEMI", "FIN", "3/4".
 * Serve una sigla corta perché la riga di anteprima è stretta.
 */
function shortPhase(code: string): string {
  return phaseLabel(code).slice(0, 4).toUpperCase()
}

/**
 * Etichetta di uno slot nell'anteprima, senza dipendere da `slotLabel` del
 * dominio: qui i ref pendenti (WINNER/LOSER) puntano all'INDICE della partita
 * nel batch e non ancora a una key vera, quindi calcoliamo direttamente il
 * codice Q1/S1/F basato sulla posizione della partita target.
 */
function previewSlot(preview: FinalStageMatch[], matchIndex: number, slot: 1 | 2): string {
  const match = preview[matchIndex]!
  const type = slot === 1 ? match.source1Type : match.source2Type
  const ref = slot === 1 ? match.source1Ref : match.source2Ref

  if (type === SOURCE_STANDING) return `${ref}ª`
  if (type === SOURCE_GROUP_STANDING) {
    let split = 0
    while (split < ref.length && !(ref[split]! >= '0' && ref[split]! <= '9')) split++
    return `${ref.substring(split)}ª GIR. ${ref.substring(0, split)}`
  }
  if (type === SOURCE_WINNER || type === SOURCE_LOSER) {
    const prefix = type === SOURCE_WINNER ? 'VINC.' : 'PERD.'
    if (isPendingRef(ref)) {
      return `${prefix} ${matchCodeInPreview(preview, pendingRefIndex(ref))}`
    }
    return `${prefix} ?`
  }
  return '—'
}

/**
 * Codice breve della partita all'indice `i` nel batch di anteprima: Q1..Q4 per
 * i quarti (in ordine d'apparizione), S1..S2 per le semifinali, F per la finale.
 */
function matchCodeInPreview(preview: FinalStageMatch[], targetIndex: number): string {
  let qCount = 0
  let sCount = 0
  for (let i = 0; i < preview.length; i++) {
    const t = preview[i]!.type
    if (t === QUARTER) {
      qCount++
      if (i === targetIndex) return `Q${qCount}`
    } else if (t === SEMIFINAL) {
      sCount++
      if (i === targetIndex) return `S${sCount}`
    } else if (t === FINAL) {
      if (i === targetIndex) return 'F'
    }
  }
  return '?'
}

function SegmentButton({
  active,
  onClick,
  disabled = false,
  children,
}: {
  active: boolean
  onClick: () => void
  disabled?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`app-title rounded px-2 py-2 text-xs transition disabled:opacity-40 ${
        active ? 'bg-action-selected text-white' : 'text-list-text-secondary hover:text-list-text'
      }`}
    >
      {children}
    </button>
  )
}
