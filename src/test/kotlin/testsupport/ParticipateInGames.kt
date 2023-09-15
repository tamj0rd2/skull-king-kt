package testsupport

import com.tamj0rd2.domain.Card

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    operator fun invoke(card: Card) = Interaction {actor ->
        actor.use<ParticipateInGames>().playCard(card)
    }

    val theFirstCardInTheirHand = Interaction { actor ->
        val card = actor.asksAbout(TheirFirstCard)
        actor.use<ParticipateInGames>().playCard(card)
    }
    val theFirstCardInHisHand = theFirstCardInTheirHand
    val theFirstCardInHerHand = theFirstCardInTheirHand
}

fun Bids(bid: Int) = Interaction { actor ->
    actor.use<ParticipateInGames>().bid(bid)
}
fun Bid(bid: Int) = Bids(bid)


val SitsAtTheTable = Interaction { actor ->
    actor.use<ParticipateInGames>().joinGame(actor.name)
}
val SitAtTheTable = SitsAtTheTable
