package testsupport

import com.tamj0rd2.domain.Card

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val SaysTheGameCanStart = Activity { actor -> actor.use<ManageGames>().startGame() }

val SaysTheRoundCanStart = Activity { actor -> actor.use<ManageGames>().startNextRound() }

val SaysTheTrickCanStart = Activity { actor -> actor.use<ManageGames>().startNextTrick() }
val SaysTheNextTrickCanStart = SaysTheTrickCanStart

object RigsTheDeck {
    data class SoThat(private val thePlayer: Actor) {
        fun willEndUpWith(vararg cards: Card) = Activity("rig the deck") { actor ->
            actor.use<ManageGames>().rigDeck(thePlayer.playerId, cards.toList())
        }
    }
}
