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
    fun `scenario - joining a game when no one else is around`() {
        with(steps) {
            `When {Actor} sits at the table`(freddy)
            `Then {Actor} sees themself at the table`(freddy)
            `Then {Actor} does not see anyone else at the table`(freddy)
            `Then {Actor} sees that they are waiting for others to join before playing`(freddy)
        }
    }


    // TODO: what about when the player has already joined the game? They shouldn't be able to join again

    @Test
    fun `scenario - joining a game when someone else is already waiting to play`() {
        with(steps) {
            // TODO: this is really 2 tests in one. I should probably write one from each perspective.
            `Given {Actor} is waiting to play`(freddy)
            `When {Actor} sits at the table`(sally)

            `Then {Actor} sees themself at the table`(sally)
            `Then {Actor} sees {Actors} at the table`(sally, listOf(freddy))
            `Then {Actor} does not see that they are waiting for others to join`(freddy)
            `Then {Actor} sees that the game has started`(sally)

            `Then {Actor} sees themself at the table`(freddy)
            `Then {Actor} sees {Actors} at the table`(freddy, listOf(sally))
            `Then {Actor} does not see that they are waiting for others to join`(freddy)
            `Then {Actor} sees that the game has started`(freddy)
        }
    }
}
