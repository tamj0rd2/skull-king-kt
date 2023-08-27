package testsupport

import com.tamj0rd2.domain.CardId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

fun PlaysCard(cardId: CardId) = Interaction { actor ->
    actor.use<ParticipateInGames>().playCard(actor.name, cardId)
}

fun Bids(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}
fun Bid(bet: Int) = Bids(bet)

val EntersTheirName = Interaction { actor ->
    actor.use<ParticipateInGames>().enterPlayerId(actor.name)
}

val JoinsARoom = Interaction { actor ->
    actor.use<ParticipateInGames>().joinDefaultRoom()
}

val SitsAtTheTable = Task(EntersTheirName, JoinsARoom)
val SitAtTheTable = SitsAtTheTable
