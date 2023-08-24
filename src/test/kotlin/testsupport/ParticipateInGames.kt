package testsupport

import com.tamj0rd2.domain.CardId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val EnterName = Interaction { actor ->
    actor.use<ParticipateInGames>().enterPlayerId(actor.name)
}

fun PlayACard(cardId: CardId) = Interaction { actor ->
    actor.use<ParticipateInGames>().playCard(actor.name, cardId)
}

private val JoinRoom = Interaction { actor ->
    actor.use<ParticipateInGames>().joinDefaultRoom()
}

fun placeABet(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}

val sitAtTheTable = Task(EnterName, JoinRoom)
