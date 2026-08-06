import type { Team } from '@/domain/models'
import { TeamCard } from '@/components/ui/TeamCard'

/**
 * Scheda squadra nella gestione: come quella pubblica, ma col voto e i tasti
 * modifica ed elimina. Porta tournament_layout_manage_teams.xml, dove il voto
 * è l'unico posto dell'app in cui compare per una squadra di torneo.
 */
export function TeamManageCard({
  teamNumber,
  team,
  onEdit,
  onDelete,
}: {
  teamNumber: number
  team: Team
  onEdit: () => void
  onDelete: () => void
}) {
  return (
    <TeamCard
      team={team}
      teamNumber={teamNumber}
      showVote
      actions={
        <>
          <IconAction label={`Modifica team ${teamNumber}`} onClick={onEdit}>
            ✎
          </IconAction>
          <IconAction label={`Elimina team ${teamNumber}`} onClick={onDelete}>
            🗑
          </IconAction>
        </>
      }
    />
  )
}

function IconAction({
  label,
  onClick,
  children,
}: {
  label: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className="grid size-[30px] shrink-0 place-items-center rounded-lg bg-icon-action
                 text-sm leading-none hover:brightness-150"
    >
      {children}
    </button>
  )
}
