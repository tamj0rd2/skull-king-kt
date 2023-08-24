package testsupport

import Card
import PlayerId

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val StartTheGame = Interaction { actor -> actor.use<ManageGames>().startGame() }

fun RigTheDeckWith(hands: Map<PlayerId, List<Card>>): Interaction = Interaction { actor ->
    actor.use<ManageGames>().rigDeck(hands)
}
