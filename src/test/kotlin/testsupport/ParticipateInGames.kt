package testsupport

import com.tamj0rd2.domain.CardId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

fun PlaysACard(cardId: CardId) = Interaction { actor ->
    actor.use<ParticipateInGames>().playCard(actor.name, cardId)
}

fun PlacesABet(bet: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().placeBet(bet)
}

val EntersTheirName = Interaction { actor ->
    actor.use<ParticipateInGames>().enterPlayerId(actor.name)
}

val JoinsARoom = Interaction { actor ->
    actor.use<ParticipateInGames>().joinDefaultRoom()
}

val SitsAtTheTable = Task(EntersTheirName, JoinsARoom)
