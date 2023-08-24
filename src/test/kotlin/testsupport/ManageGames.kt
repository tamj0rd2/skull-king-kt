package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.PlayerId

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val StartTheGame = Interaction { actor -> actor.use<ManageGames>().startGame() }

fun RigTheDeckWith(hands: Map<PlayerId, List<Card>>): Interaction = Interaction { actor ->
    actor.use<ManageGames>().rigDeck(hands)
}
