import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestMethodOrder
import testsupport.*
import kotlin.test.Test

interface Driver : ApplicationDriver, GameMasterDriver

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AppTestContract {
    abstract val makeDriver: () -> Driver

    private val freddyFirstPlayer by lazy { Actor("Freddy First").whoCan(ParticipateInGames(makeDriver())) }
    private val sallySecondPlayer by lazy { Actor("Sally Second").whoCan(ParticipateInGames(makeDriver())) }
    private val garyGameMaster by lazy { Actor("Gary Game Master").whoCan(StartGames(makeDriver())) }
    private val steps = Steps()

    @Test
    @Order(1)
    fun `scenario - joining a game when no one else is waiting`() {
        with(steps) {
            `When {Actor} sits at the table`(freddyFirstPlayer)
            `Then {Actor} sees themself at the table`(freddyFirstPlayer)
            `Then {Actor} does not see anyone else at the table`(freddyFirstPlayer)
            `Then {Actor} sees that they are waiting for others to join`(freddyFirstPlayer)
        }
    }

    @TestFactory
    @Order(2)
    fun `scenario - joining a game when someone else is already waiting to play`(): List<DynamicTest> {
        with(steps) {
            val personWaiting = freddyFirstPlayer
            val personJoining = sallySecondPlayer

            // NOTE: this is essentially the Background in gherkin
            `Given {Actor} is at the table`(personWaiting)
            `When {Actor} sits at the table`(personJoining)

            return listOf(
                DynamicTest.dynamicTest("from ${personWaiting.name}'s perspective") {
                    `Then {Actor} sees themself at the table`(personWaiting)
                    `Then {Actor} sees {Actors} at the table`(personWaiting, listOf(personJoining))
                    `Then {Actor} does not see that they are waiting for others to join`(personWaiting)
                    `Then {Actor} does not see that the game has started`(personWaiting)
                },

                DynamicTest.dynamicTest("from ${personJoining.name}'s perspective") {
                    `Then {Actor} sees themself at the table`(personJoining)
                    `Then {Actor} sees {Actors} at the table`(personJoining, listOf(personWaiting))
                    `Then {Actor} does not see that they are waiting for others to join`(personJoining)
                    `Then {Actor} does not see that the game has started`(personJoining)
                },
            )
        }
    }

    @Test
    @Order(3)
    fun `scenario - starting round 1`() {
        with(steps) {
            `Given {Actor} is at the table`(freddyFirstPlayer)
            `Given {Actor} is at the table`(sallySecondPlayer)
            `When {Actor} says the game can start`(garyGameMaster)
            `Then {Actor} sees that the game has started`(freddyFirstPlayer)
            `Then {Actor} sees that the game has started`(sallySecondPlayer)
            `Then {Actor} has {Count} cards`(freddyFirstPlayer, 1)
            `Then {Actor} has {Count} cards`(sallySecondPlayer, 1)
        }
    }

    @Test
    @Order(4)
    fun `scenario - bids are shown after completing bidding`() {
        with(steps) {
            val bets = mapOf(freddyFirstPlayer.name to 1, sallySecondPlayer.name to 0)

            `Given {Actors} are in a game started by {Actor}`(listOf(freddyFirstPlayer, sallySecondPlayer), garyGameMaster)
            `When {Actor} places a bet of {Bet}`(freddyFirstPlayer, bets[freddyFirstPlayer.name]!!)
            `When {Actor} places a bet of {Bet}`(sallySecondPlayer, bets[sallySecondPlayer.name]!!)
            `Then {Actor} sees the placed {Bets}`(freddyFirstPlayer, bets)
            `Then {Actor} sees the placed {Bets}`(sallySecondPlayer, bets)
        }
    }

    @Test
    @Order(5)
    fun `scenario - bids are not shown if not everyone has finished bidding`() {
        with(steps) {
            `Given {Actors} are in a game started by {Actor}`(
                listOf(freddyFirstPlayer, sallySecondPlayer),
                garyGameMaster
            )
            `When {Actor} places a bet of {Bet}`(freddyFirstPlayer, 1)
            `Then {Actor} can see they have made their bet`(freddyFirstPlayer)
            `Then {Actor} can see that {Actor} has made a bet`(sallySecondPlayer, freddyFirstPlayer)
            `Then {Actor} cannot see anyone's actual bet`(sallySecondPlayer)
        }
    }

    //@Test
    //@Order(6)
    //fun `scenario - taking tricks once bidding is complete`() {
    //    with(steps) {
    //        // Given the game master has rigged the deck
    //        `Given {Actors} are in a game started by {Actor}`(
    //            listOf(freddyFirstPlayer, sallySecondPlayer),
    //            garyGameMaster
    //        )
    //        `Given all {Bets} have been taken`(mapOf(freddyFirstPlayer.name to 0, sallySecondPlayer.name to 1))
    //        `When {Actor} places a bet of {Bet}`(freddyFirstPlayer, 1)
    //        //`Then {Actor} can see their own bet`(freddyFirstPlayer)
    //        `Then {Actor} can see that {Actor} has made a bet`(sallySecondPlayer, freddyFirstPlayer)
    //        //`Then {Actor} cannot see what {Actor}'s bet is`(sallySecondPlayer)
    //    }
    //}
}
