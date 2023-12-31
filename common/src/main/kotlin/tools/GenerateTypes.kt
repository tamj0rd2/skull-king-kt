package tools

import com.tamj0rd2.domain.*
import com.tamj0rd2.messaging.Message
import dev.adamko.kxstsgen.KxsTsGenerator

fun main() {
    val output = KxsTsGenerator().generate(
        Card.serializer(),
        CardWithPlayability.serializer(),
        PlayerCommand.serializer(),
        GamePhase.serializer(),
        RoundPhase.serializer(),
        PlayerId.serializer(),
        Message.serializer(),
        PlayerGameState.serializer(),
    ).replace(";", "")

    println(output)
}
