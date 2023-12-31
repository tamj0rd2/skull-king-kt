package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameMasterCommand

class ManageGames(driver: GameMasterDriver) : Ability, GameMasterDriver by driver

val SaysTheGameCanStart = Activity("start the game") { actor ->
    actor.use<ManageGames>().perform(GameMasterCommand.StartGame)
    actor.use<ManageGames>().perform(GameMasterCommand.StartNextRound)
}

val SaysTheRoundCanStart =
    Activity("start the round") { actor -> actor.use<ManageGames>().perform(GameMasterCommand.StartNextRound) }

val SaysTheTrickCanStart =
    Activity("start the trick") { actor -> actor.use<ManageGames>().perform(GameMasterCommand.StartNextTrick) }
val SaysTheNextTrickCanStart = SaysTheTrickCanStart

object RigsTheDeck {
    data class SoThat(private val thePlayer: Actor) {
        fun willEndUpWith(vararg cards: Card) = Activity("rig the deck") { actor ->
            actor.use<ManageGames>().perform(GameMasterCommand.RigDeck(thePlayer.playerId, cards.toList()))
        }
    }
}
