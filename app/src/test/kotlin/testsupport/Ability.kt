package testsupport

import com.github.michaelbull.result.Result
import com.tamj0rd2.domain.CommandError
import com.tamj0rd2.domain.GameMasterCommand
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerState

interface Ability

interface ApplicationDriver {
    fun perform(command: PlayerCommand): Result<Unit, CommandError>
    val state: PlayerState
}

interface GameMasterDriver {
    fun perform(command: GameMasterCommand)
}
