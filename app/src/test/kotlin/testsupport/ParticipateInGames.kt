package testsupport

import com.github.michaelbull.result.getOrThrow
import com.tamj0rd2.domain.*
import com.tamj0rd2.webapp.CustomJsonSerializer.asJsonObject
import com.tamj0rd2.webapp.CustomJsonSerializer.asPrettyJsonString

class ParticipateInGames(driver: ApplicationDriver) : Ability, ApplicationDriver by driver

val Play = Plays
val Playing = Plays

private fun Actor.performPlayerCommand(command: PlayerCommand) =
    use<ParticipateInGames>().perform(command).getOrThrow {
        logger.warn("$it")
        it.reason.asException()
    }

val Actor.gameState get() = use<ParticipateInGames>().state

internal fun PlayerGameState.debug() = asJsonObject().asPrettyJsonString()

object Plays {
    operator fun invoke(card: Card) = Activity("play ${card.name}") { actor ->
        actor.performPlayerCommand(PlayerCommand.PlayCard(actor.playerId, card.name))
    }

    val theirFirstPlayableCard = Activity("play their first playable card") { actor ->
        val state = actor.gameState
        val hand = state.hand
        val card = hand.firstOrNull { it.isPlayable }?.card
            ?: error("${actor.playerId} has no playable cards in their hand\n\n${state.debug()}")
        actor.performPlayerCommand(PlayerCommand.PlayCard(actor.playerId, card.name))
    }
}

fun Bids(bid: Int) = Activity("bid $bid") { actor ->
    actor.performPlayerCommand(PlayerCommand.PlaceBid(actor.playerId, Bid.of(bid)))
}

fun Bid(bid: Int) = Bids(bid)
fun Bidding(bid: Int) = Bids(bid)

val SitsAtTheTable = Activity("join the game") { actor ->
    actor.performPlayerCommand(PlayerCommand.JoinGame(actor.playerId))
}
val SitAtTheTable = SitsAtTheTable
val SittingAtTheTable = SitsAtTheTable

val Actor.playerId get() = PlayerId(name.lowercase().replace(" ", "_"))
