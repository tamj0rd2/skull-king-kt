import com.tamj0rd2.domain.Card
import com.tamj0rd2.domain.CardWithPlayability
import com.tamj0rd2.domain.GameState
import com.tamj0rd2.domain.PlayerCommand
import com.tamj0rd2.domain.PlayerId
import com.tamj0rd2.domain.RoundPhase
import com.tamj0rd2.webapp.Message
import com.tamj0rd2.webapp.Notification
import dev.adamko.kxstsgen.KxsTsGenerator
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists

fun main(args: Array<String>) {
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

    val builtFiles = setOf(
        "../resources/public/entry.js",
        "../resources/public/entry.js.map",
        "../resources/public/entry.css",
        "../resources/public/entry.css.map",
        "../resources/public/game.js",
        "../resources/public/game.js.map",
    )
    builtFiles.forEach { Paths.get(it).deleteIfExists() }

    setOf("./src/frontend", "./src/svelte-frontend").forEach {frontend ->
        val cwd = Paths.get(frontend).toAbsolutePath().toFile()
        println("npm install".runCommand(cwd))
        File("$frontend/generated_types.ts").writeText(output)
        println("npm run build".runCommand(cwd))
        println("Built $frontend")
    }

    println("Done.")
}

private fun String.runCommand(workingDir: File): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(10, TimeUnit.SECONDS)
    val result = proc.inputStream.bufferedReader().readText()
    if (result.isEmpty()) error("Failed to run command: $this\n${proc.errorStream.bufferedReader().readText()}")
    proc.exitValue().let { if (it != 0) error("Failed to run command: $this\nExit code $it\n${proc.errorStream.bufferedReader().readText()}\n$result") }
    return result
}
