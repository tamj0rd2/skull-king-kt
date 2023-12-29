package testsupport

import com.github.michaelbull.result.Result
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.CommandError
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.DisplayBid
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayedCard
import com.tamj0rd2.domain.PlayerGameState
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundNumber
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.domain.TrickNumber

interface Ability

interface ApplicationDriver {
    fun perform(command: PlayerCommand): Result<Unit, CommandError>
    val state: PlayerGameState
}

interface GameMasterDriver {
    fun perform(command: GameMasterCommand)
}
