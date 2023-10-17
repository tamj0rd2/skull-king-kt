package testsupport

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.GameException
import com.tamj0rd2.domain.PlayerId

class ParticipateInGames(driver: ApplicationDriver): Ability, ApplicationDriver by driver

val Play = Plays
object Plays {
    operator fun invoke(card: Card) = Activity("play ${card.name}") {actor ->
        actor.use<ParticipateInGames>().playCard(card)
    }

    val theirFirstPlayableCard = Activity("play their first playable card") { actor ->
        val ability = actor.use<ParticipateInGames>()
        val hand = actor.asksAbout(TheirHand)

        /*
TODO: this doesn't work because sally is choosing which card to play before she knows which card freddy has played
TODO: Before freddy officially finishes playing his card, sally first needs to confirm that she has received his update.
08:57:51.922 INFO  >>> TEST <<< -- Freddy First is attempting to 'play their first playable card'
08:57:51.922 INFO  freddy_first:wsHandler -- received client message from freddy_first: {"type":"MessageFromClient$CardPlayed","cardName":"SkullKing"}
08:57:51.923 INFO  freddy_first:wsHandler -- sending message to freddy_first: Multi(messages=[CardPlayed(playerId=freddy_first, card=skullking, nextPlayer=sally_second)])
08:57:51.923 INFO  sally_second:wsHandler -- sending message to sally_second: Multi(messages=[CardPlayed(playerId=freddy_first, card=skullking, nextPlayer=sally_second), YourTurn(cards={Pirate=true, Blue-4=true})])
08:57:51.923 INFO  freddy_first:wsClient -- received message: Multi(messages=[CardPlayed(playerId=freddy_first, card=skullking, nextPlayer=sally_second)])
08:57:51.923 INFO  >>> TEST <<< -- Sally Second is attempting to 'play their first playable card'
08:57:51.924 INFO  sally_second:wsClient -- received message: Multi(messages=[CardPlayed(playerId=freddy_first, card=skullking, nextPlayer=sally_second), YourTurn(cards={Pirate=true, Blue-4=true})])
08:57:51.929 INFO  io.undertow -- stopping server: Undertow - 2.3.7.Final

java.lang.IllegalStateException: card Pirate playability not found in {Blue-7=true}
         */

        // TODO:
        val firstPlayableCard = hand.firstOrNull { ability.isCardPlayable(it) } ?: throw GameException.CannotPlayCard("No playable cards in hand")
        ability.playCard(firstPlayableCard)
    }
}

fun Bids(bid: Int) = Activity("bid $bid") { actor ->
    actor.use<ParticipateInGames>().bid(bid)
}
fun Bid(bid: Int) = Bids(bid)


val SitsAtTheTable = Activity("join the game") { actor ->
    actor.use<ParticipateInGames>().joinGame(actor.playerId)
}
val SitAtTheTable = SitsAtTheTable

val Actor.playerId get() = PlayerId(name.lowercase().replace(" ", "_"))
