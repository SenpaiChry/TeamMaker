import { useCallback, useEffect, useRef } from 'react'

/**
 * Tasto che esegue l'azione al tocco e poi la ripete finché resta premuto.
 * Porta RepeatListener di ActivityScorecard, stessi tempi.
 */
const INITIAL_DELAY_MS = 400
const REPEAT_INTERVAL_MS = 80

export function RepeatButton({ onTrigger, label }: { onTrigger: () => void; label: string }) {
  const delayRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // `onTrigger` cambia a ogni render (dipende dal punteggio): tenerlo in un ref
  // evita di dover riavviare i timer mentre il tasto è premuto.
  const triggerRef = useRef(onTrigger)
  triggerRef.current = onTrigger

  const stop = useCallback(() => {
    if (delayRef.current !== null) clearTimeout(delayRef.current)
    if (intervalRef.current !== null) clearInterval(intervalRef.current)
    delayRef.current = null
    intervalRef.current = null
  }, [])

  useEffect(() => stop, [stop])

  const start = () => {
    triggerRef.current()
    stop()
    delayRef.current = setTimeout(() => {
      intervalRef.current = setInterval(() => triggerRef.current(), REPEAT_INTERVAL_MS)
    }, INITIAL_DELAY_MS)
  }

  return (
    <button
      type="button"
      aria-label={label === '+' ? 'Aggiungi' : 'Togli'}
      onPointerDown={(e) => {
        e.preventDefault()
        start()
      }}
      onPointerUp={stop}
      onPointerLeave={stop}
      onPointerCancel={stop}
      className="size-12 rounded-lg border border-score-panel-border bg-score-panel
                 text-2xl leading-none select-none hover:bg-score-panel-border"
    >
      {label}
    </button>
  )
}
