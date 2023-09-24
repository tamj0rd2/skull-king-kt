package testsupport

import com.tamj0rd2.domain.Card

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val SaysTheGameCanStart = Interaction { actor -> actor.use<ManageGames>().startGame() }

val SaysTheRoundCanStart = Interaction { actor -> actor.use<ManageGames>().startNextRound() }

val SaysTheTrickCanStart = Interaction { actor -> actor.use<ManageGames>().startNextTrick() }
val SaysTheNextTrickCanStart = Interaction { actor -> actor.use<ManageGames>().startNextTrick() }

object RigsTheDeck {
    data class SoThat(private val thePlayer: Actor) {
        fun willEndUpWith(vararg cards: Card) = Interaction { actor ->
            actor.use<ManageGames>().rigDeck(thePlayer.playerId, cards.toList())
        }
    }
}
