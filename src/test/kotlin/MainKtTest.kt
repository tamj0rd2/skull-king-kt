import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import testsupport.*
import testsupport.adapters.DomainDriver
import kotlin.test.Test

class MainKtTest {
    @Test
    fun `scenario - starting a new game`() {
        val app = App()

        val freddy = Actor("Freddy the first player")
            .can(AccessTheApplication(DomainDriver(app)))

        val sally = Actor("Sally the second player")
            .can(AccessTheApplication(DomainDriver(app)))

        freddy.attemptsTo(
            joinARoom(freddy.name),
            ensureThat(playersInRoom(), hasElement(freddy.name) and !hasElement(sally.name)),
            ensureThat(waitingForMorePlayers(), equalTo(true)),
        )

        sally.attemptsTo(
            joinARoom(sally.name),
            ensureThat(playersInRoom(), hasElement(freddy.name) and hasElement(sally.name)),
            ensureThat(waitingForMorePlayers(), equalTo(false)),
        )

        freddy.attemptsTo(ensureThat(playersInRoom(), hasElement(freddy.name) and hasElement(sally.name)))
    }
}

fun <T> ensureThat(question: Question<T>, matcher: Matcher<T>, message: String? = null) = Activity { abilities ->
    val answer = question.ask(abilities)
    assertThat(answer, matcher) { message.orEmpty() }
}
