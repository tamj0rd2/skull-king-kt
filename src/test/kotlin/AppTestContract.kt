import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import testsupport.*
import kotlin.test.Test

interface Driver : ApplicationDriver, GameMasterDriver

abstract class AppTestContract {
    abstract val makeDriver: () -> Driver

    private val freddyFirstPlayer by lazy { Actor("Freddy First").whoCan(ParticipateInGames(makeDriver())) }
    private val sallySecondPlayer by lazy { Actor("Sally Second").whoCan(ParticipateInGames(makeDriver())) }
    private val garyGameMaster by lazy { Actor("Gary Game Master").whoCan(StartGames(makeDriver())) }
    private val steps = Steps()

    @Test
    fun `scenario - joining a game when no one else is waiting`() {
        with(steps) {
            `When {Actor} sits at the table`(freddyFirstPlayer)
            `Then {Actor} sees themself at the table`(freddyFirstPlayer)
            `Then {Actor} does not see anyone else at the table`(freddyFirstPlayer)
            `Then {Actor} sees that they are waiting for others to join`(freddyFirstPlayer)
        }
    }

    @TestFactory
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
}
