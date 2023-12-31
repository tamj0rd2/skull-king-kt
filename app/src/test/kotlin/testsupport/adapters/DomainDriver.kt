package testsupport.adapters

import com.github.michaelbull.result.Result
import com.tamj0rd2.domain.*
import testsupport.ApplicationDriver
import testsupport.GameMasterDriver

class DomainDriver(private val game: Game) : ApplicationDriver, GameMasterDriver {
    override lateinit var state: PlayerState

    override fun perform(command: PlayerCommand): Result<Unit, CommandError> {
        if (command is PlayerCommand.JoinGame) {
            state = PlayerState.ofPlayer(command.actor, game.allEventsSoFar)
            game.subscribeToGameEvents { events, _ -> state = state.handle(events) }
        }

        return game.perform(command)
    }

    override fun perform(command: GameMasterCommand) {
        game.perform(command)
    }
}