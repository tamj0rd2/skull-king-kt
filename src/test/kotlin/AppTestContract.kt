import jakarta.servlet.Registration.Dynamic
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import testsupport.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test

abstract class AppTestContract(makeDriver: (app: App) -> ApplicationDriver, app: App = App()) {
    private val freddy = Actor("Freddy First")
        .whoCan(ParticipateInGames(makeDriver(app)))

    private val sally = Actor("Sally Second")
        .whoCan(ParticipateInGames(makeDriver(app)))

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
                DynamicTest.dynamicTest("from the person waiting's perspective") {
                    `Then {Actor} sees themself at the table`(personWaiting)
                    `Then {Actor} sees {Actors} at the table`(personWaiting, listOf(personJoining))
                    `Then {Actor} does not see that they are waiting for others to join`(personWaiting)
                    `Then {Actor} sees that the game has started`(personWaiting)
                },

                DynamicTest.dynamicTest("from the joiner's perspective") {
                    `Then {Actor} sees themself at the table`(personJoining)
                    `Then {Actor} sees {Actors} at the table`(personJoining, listOf(personWaiting))
                    `Then {Actor} does not see that they are waiting for others to join`(personJoining)
                    `Then {Actor} sees that the game has started`(personJoining)
                },
            )
        }
    }
}
