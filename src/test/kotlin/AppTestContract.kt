import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import testsupport.*
import kotlin.test.Test

abstract class AppTestContract {
    abstract val makeDriver: () -> ApplicationDriver

    private val freddy by lazy { Actor("Freddy First").whoCan(ParticipateInGames(makeDriver())) }
    private val sally by lazy { Actor("Sally Second").whoCan(ParticipateInGames(makeDriver())) }
    private val steps = Steps()

    @Test
    fun `scenario - joining a game when no one else is waiting`() {
        with(steps) {
            `When {Actor} sits at the table`(freddy)
            `Then {Actor} sees themself at the table`(freddy)
            `Then {Actor} does not see anyone else at the table`(freddy)
            `Then {Actor} sees that they are waiting for others to join before playing`(freddy)
        }
    }

    @TestFactory
    fun `scenario - joining a game when someone else is already waiting to play`(): List<DynamicTest> {
        with(steps) {
            val personWaiting = freddy
            val personJoining = sally

            // NOTE: this is essentially the Background in gherkin
            `Given {Actor} is waiting to play`(personWaiting)
            `When {Actor} sits at the table`(personJoining)

            return listOf(
                DynamicTest.dynamicTest("from ${personWaiting.name}'s perspective") {
                    `Then {Actor} sees themself at the table`(personWaiting)
                    `Then {Actor} sees {Actors} at the table`(personWaiting, listOf(personJoining))
                    `Then {Actor} does not see that they are waiting for others to join`(personWaiting)
                    `Then {Actor} sees that the game has started`(personWaiting)
                },

                DynamicTest.dynamicTest("from ${personJoining.name}'s perspective") {
                    `Then {Actor} sees themself at the table`(personJoining)
                    `Then {Actor} sees {Actors} at the table`(personJoining, listOf(personWaiting))
                    `Then {Actor} does not see that they are waiting for others to join`(personJoining)
                    `Then {Actor} sees that the game has started`(personJoining)
                },
            )
        }
    }

    @Test
    fun `scenario - betting in round 1`() {
        with(steps) {
            `Given {Actor} is waiting to play`(freddy)
            `When {Actor} sits at the table`(sally)
            `Then {Actor} has {Count} cards`(freddy, 1)
            `Then {Actor} has {Count} cards`(sally, 1)
        }
    }
}
