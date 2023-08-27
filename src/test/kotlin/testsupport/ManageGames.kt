package testsupport

import com.tamj0rd2.domain.Card

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val StartsTheGame = Interaction { actor -> actor.use<ManageGames>().startGame() }

object RigsTheDeck {
    data class SoThat(private val thePlayer: Actor) {
        fun willEndUpWith(vararg cards: Card) = Interaction { actor ->
            actor.use<ManageGames>().rigDeck(thePlayer.name, cards.toList())
        }
    }
}
