package testsupport

import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber

interface Ability

interface ApplicationDriver {
    fun perform(command: PlayerCommand)

    val winsOfTheRound: Map<PlayerId, Int>
    val trickWinner: PlayerId?
    val currentPlayer: PlayerId?
    val trickNumber: TrickNumber
    val roundNumber: RoundNumber
    val trick: List<PlayedCard>
    val roundPhase: RoundPhase?
    val gameState: GameState?
    val playersInRoom: List<PlayerId>
    val hand: List<CardWithPlayability>
    val bids: Map<PlayerId, DisplayBid>
}

interface GameMasterDriver {
    fun perform(command: GameMasterCommand)
}
