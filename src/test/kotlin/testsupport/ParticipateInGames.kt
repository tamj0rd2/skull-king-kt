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

    val theFirstCardInHisHand = theFirstCardInTheirHand
    val theFirstCardInHerHand = theFirstCardInTheirHand
}

private fun playsCard(cardId: CardId) = Interaction { actor ->
    actor.use<ParticipateInGames>().playCard(cardId)
}

fun Bids(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}
fun Bid(bet: Int) = Bids(bet)

private val EntersTheirName = Interaction { actor ->
    actor.use<ParticipateInGames>().enterPlayerId(actor.name)
}

private val JoinsARoom = Interaction { actor ->
    actor.use<ParticipateInGames>().joinDefaultRoom()
}

val SitsAtTheTable = Task(EntersTheirName, JoinsARoom)
val SitAtTheTable = SitsAtTheTable
