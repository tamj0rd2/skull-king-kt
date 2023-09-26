package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.PlayerId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    operator fun invoke(card: Card) = Interaction {actor ->
        actor.use<ParticipateInGames>().playCard(card)
    }

    val theirFirstPlayableCard = Interaction { actor ->
        val ability = actor.use<ParticipateInGames>()
        val hand = actor.asksAbout(TheirHand)
        val firstPlayableCard = hand.firstOrNull { ability.isCardPlayable(it) } ?: throw GameException.CannotPlayCard("No playable card in hand")
        ability.playCard(firstPlayableCard)
    }
}

fun Bids(bid: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().bid(bid)
}
fun Bid(bid: Int) = Bids(bid)


val SitsAtTheTable = Interaction { actor ->
    actor.use<ParticipateInGames>().joinGame(actor.playerId)
}
val SitAtTheTable = SitsAtTheTable

val Actor.playerId get() = PlayerId(name.lowercase().replace(" ", "_"))
