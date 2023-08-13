import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import testsupport.*
import kotlin.test.Test

abstract class AppTestContract(appDriver: ApplicationDriver) {
    private val freddy = Actor("Freddy the first player")
        .isAbleTo(AccessTheApplication(appDriver))

    private val sally = Actor("Sally the second player")
        .isAbleTo(AccessTheApplication(appDriver))

   @Test
   fun `scenario - joining a game when no one else is around`() {
       with(Steps) {
           `When {actor} sits at the table`(freddy)
           `Then {actor} sees themself at the table`(freddy)
           `Then {actor} does not see anyone else at the table`(freddy)
           `Then {actor} sees that they are waiting for others to join before playing`(freddy)
       }
   }

    @Test
    fun `scenario - joining a game when someone else is already waiting to play`() {
        with(Steps) {
            `Given {actor} is waiting to play`(freddy)
            `When {actor} sits at the table`(sally)

            `Then {actor} sees themself at the table`(sally)
            `Then {actor} sees {other actors} at the table`(sally, listOf(freddy))
            `Then {actor} sees that the game has started`(sally)

            `Then {actor} sees themself at the table`(freddy)
            `Then {actor} sees {other actors} at the table`(freddy, listOf(sally))
            `Then {actor} sees that the game has started`(freddy)
        }
    }
}

fun <T> ensureThat(question: Question<T>, matcher: Matcher<T>, message: String? = null) = Activity { abilities ->
    val answer = question.ask(abilities)
    assertThat(answer, matcher) { message.orEmpty() }
}

fun includes(vararg actors: Actor) = actors.toList()
    .map{it.name}
    .drop(1)
    .fold(hasElement(actors[0].name)) { acc, name -> acc.and(hasElement(name)) }

object Steps {
    fun `Given {actor} is waiting to play`(actor: Actor) = actor.attemptsTo(sitAtTheTable)

    fun `When {actor} sits at the table`(actor: Actor) = actor.attemptsTo(sitAtTheTable)

    fun `Then {actor} sees themself at the table`(actor: Actor) =
        actor.attemptsTo(ensureThat(playersAtTheTable, hasElement(actor.name)))

    fun `Then {actor} does not see anyone else at the table`(actor: Actor) =
        actor.attemptsTo(ensureThat(playersAtTheTable, hasSize(equalTo(1))))

    fun `Then {actor} does not see {other actors} at the table`(actor: Actor, otherActors: Collection<Actor>) =
        actor.attemptsTo(ensureThat(playersAtTheTable, !includes(*otherActors.toTypedArray())))

    fun `Then {actor} sees {other actors} at the table`(actor: Actor, otherActors: Collection<Actor>) =
        actor.attemptsTo(ensureThat(playersAtTheTable, includes(*otherActors.toTypedArray())))

    fun `Then {actor} sees that they are waiting for others to join before playing`(actor: Actor) =
        actor.attemptsTo(ensureThat(waitingForMorePlayers, equalTo(true)))

    fun `Then {actor} sees that the game has started`(actor: Actor) =
        actor.attemptsTo(ensureThat(hasGameStarted, equalTo(true)))
}