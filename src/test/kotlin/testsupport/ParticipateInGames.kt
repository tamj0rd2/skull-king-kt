package testsupport

import com.tamj0rd2.domain.CardId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    fun card(cardId: CardId) = playsCard(cardId)
    val theFirstCardInTheirHand = Interaction { actor ->
        val firstCard = TheirHand.answeredBy(actor).first()
        actor(card(firstCard.id))
    }
}

private fun playsCard(cardId: CardId) = Interaction { actor ->
    actor.use<ParticipateInGames>().playCard(cardId)
}

fun Bids(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}
fun Bid(bet: Int) = Bids(bet)


val SitsAtTheTable = Interaction { actor ->
    actor.use<ParticipateInGames>().joinGame(actor.name)
}
val SitAtTheTable = SitsAtTheTable
