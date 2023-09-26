package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.PlayerId
import java.time.Instant.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    operator fun invoke(card: Card) = Interaction {actor ->
        actor.use<ParticipateInGames>().playCard(card)
    }

    val theirFirstPlayableCard = Interaction { actor ->
        val ability = actor.use<ParticipateInGames>()
        val hand = actor.asksAbout(TheirHand)

        val firstPlayableCard = retry(100.milliseconds) {
            hand.firstOrNull { ability.isCardPlayable(it) } ?: throw GameException.CannotPlayCard("No playable card in hand")
        }
        ability.playCard(firstPlayableCard)
    }
}

fun <T> retry(total: Duration, block: () -> T): T {
    val endTime = now().plusMillis(total.inWholeMilliseconds)
    var lastError: Throwable?

    do {
        try {
            return block()
        } catch (e: Throwable) {
            lastError = e
        }
    } while (now() < endTime)

    throw lastError!!
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
