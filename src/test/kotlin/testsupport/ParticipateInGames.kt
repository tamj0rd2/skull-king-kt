package testsupport

import com.tamj0rd2.domain.Bid
import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.Command.PlayerCommand
import com.tamj0rd2.domain.PlayerId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    operator fun invoke(card: Card) = Activity("play ${card.name}") {actor ->
        actor.use<ParticipateInGames>().perform(PlayerCommand.PlayCard(actor.playerId, card.name))
    }

    val theirFirstPlayableCard = Activity("play their first playable card") { actor ->
        val ability = actor.use<ParticipateInGames>()
        val card = ability.hand.firstOrNull { it.isPlayable }?.card ?: error("no playable cards in hand")
        ability.perform(PlayerCommand.PlayCard(actor.playerId, card.name))
    }
}

fun Bids(bid: Int) = Activity("bid $bid") { actor ->
    actor.use<ParticipateInGames>().perform(PlayerCommand.PlaceBid(actor.playerId, Bid(bid)))
}
fun Bid(bid: Int) = Bids(bid)

val SitsAtTheTable = Activity("join the game") { actor ->
    actor.use<ParticipateInGames>().perform(PlayerCommand.JoinGame(actor.playerId))
}
val SitAtTheTable = SitsAtTheTable

val Actor.playerId get() = PlayerId(name.lowercase().replace(" ", "_"))
