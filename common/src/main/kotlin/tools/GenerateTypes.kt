package tools

import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.messaging.Message
import com.tamj0rd2.messaging.Notification
import dev.adamko.kxstsgen.KxsTsGenerator
import java.io.File

fun main() {
    val output = KxsTsGenerator().generate(
        Card.serializer(),
        CardWithPlayability.serializer(),
        PlayerCommand.serializer(),
        GameState.serializer(),
        RoundPhase.serializer(),
        PlayerId.serializer(),
        Notification.serializer(),
        Message.serializer()
    ).replace(";", "")

    println(output)
}
