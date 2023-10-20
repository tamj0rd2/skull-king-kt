package testsupport

import com.tamj0rd2.domain.Card

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val SaysTheGameCanStart = Activity("start the game") { actor ->
    actor.use<ManageGames>().startGame()
    actor.use<ManageGames>().startNextRound()
}

val SaysTheRoundCanStart = Activity("start the round") { actor -> actor.use<ManageGames>().startNextRound() }

val SaysTheTrickCanStart = Activity("start the trick") { actor -> actor.use<ManageGames>().startNextTrick() }
val SaysTheNextTrickCanStart = SaysTheTrickCanStart

object RigsTheDeck {
    data class SoThat(private val thePlayer: Actor) {
        fun willEndUpWith(vararg cards: Card) = Activity("rig the deck") { actor ->
            actor.use<ManageGames>().rigDeck(thePlayer.playerId, cards.toList())
        }
    }
}
