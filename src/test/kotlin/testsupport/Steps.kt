package testsupport

import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Clock

interface Steps {
    fun `Given {Actor} is at the table`(actor: Actor) = actor.attemptsTo(sitAtTheTable)

    fun `When {Actor} sits at the table`(actor: Actor) = actor.attemptsTo(sitAtTheTable)

    fun `Then {Actor} sees themself at the table`(actor: Actor) =
        actor.attemptsTo(ensureThat(playersAtTheTable, hasElement(actor.name)))

    fun `Then {Actor} does not see anyone else at the table`(actor: Actor) =
        actor.attemptsTo(ensureThat(playersAtTheTable, hasSize(equalTo(1))))

    fun `Then {Actor} sees {Actors} at the table`(actor: Actor, actors: Collection<Actor>) =
        actor.attemptsTo(ensureThat(playersAtTheTable, includes(*actors.toTypedArray())))

    fun `Then {Actor} sees that they are waiting for others to join`(actor: Actor) =
        actor.attemptsTo(ensureThat(waitingForMorePlayers, equalTo(true)))

    fun `Then {Actor} does not see that they are waiting for others to join`(actor: Actor) =
        actor.attemptsTo(ensureThat(waitingForMorePlayers, equalTo(false)))

    fun `Then {Actor} sees that the game has started`(actor: Actor) =
        actor.attemptsTo(ensureThat(hasGameStarted, equalTo(true)))

    fun `Then {Actor} does not see that the game has started`(actor: Actor) =
        actor.attemptsTo(ensureThat(hasGameStarted, equalTo(false)))


    fun `Then {Actor} has {Count} cards`(actor: Actor, cardCount: Int) {
        actor.attemptsTo(ensureThat(theirCardCount, equalTo(cardCount)))
    }

    fun `When {Actor} says the game can start`(actor: Actor) = actor.attemptsTo(startTheGame)
}

fun Steps(): Steps = Proxy.newProxyInstance(
        Steps::class.java.classLoader, arrayOf(Steps::class.java),
        DynamicInvocationHandler(object : Steps {})
    ) as Steps

fun <T> ensureThat(question: Question<T>, matcher: Matcher<T>) = Activity { abilities ->
    val clock = Clock.systemDefaultZone()
    val startTime = clock.instant()
    val mustEndBy = startTime.plusSeconds(2)

    do {
        try {
            val answer = question.ask(abilities)
            assertThat(answer, matcher)
            break
        } catch (e: AssertionError) {
            if (clock.instant() > mustEndBy) throw e
            Thread.sleep(100)
        }
    } while (true)
}

fun includes(vararg actors: Actor) = actors.toList()
    .map { it.name }
    .drop(1)
    .fold(hasElement(actors[0].name)) { acc, name -> acc.and(hasElement(name)) }


private class DynamicInvocationHandler(private val real: Steps) : InvocationHandler {
    private var phase = "unset"
    private val possiblePhases = setOf("Given", "When", "Then")

    override fun invoke(proxy: Any?, method: Method, args: Array<Any>?): Any? =
        try {
            getGherkin(method, args)?.let { println(it) }
            method.invoke(real, *(args ?: emptyArray()))
        } catch(e: Throwable) {
            e.cause?.let { throw it } ?: throw e
        }

    private fun getGherkin(method: Method, args: Array<Any>?): String? {
        val previousPhase = phase
        phase = method.name.substringBefore(" ")
        if (!possiblePhases.contains(phase)) return null

        val methodName = if (phase == previousPhase) method.name.replaceFirst(phase, "And") else method.name

        if (args.isNullOrEmpty()) return methodName

        return Regex("\\{([a-zA-Z0-9]+)}")
            .findAll(methodName)
            .map { it.groupValues[1] }
            .foldIndexed(methodName) { index, acc, placeholder ->
                acc.replace("{$placeholder}", when(placeholder) {
                    "Actor" -> (args[index] as Actor).name
                    "Actors" -> (args[index] as Collection<*>).map { (it as Actor).name }.joinReadable()
                    "Count" -> (args[index] as Number).toString()
                    else -> error("Unknown placeholder: $placeholder")
                })
            }
    }
}

private fun <T> List<T>.joinReadable(): String = when (size) {
    0 -> ""
    1 -> first().toString()
    2 -> "${first()} and ${last()}"
    else -> "${dropLast(1).joinToString(", ")} and ${last()}"
}
