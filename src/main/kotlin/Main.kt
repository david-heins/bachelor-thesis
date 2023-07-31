import com.github.ajalt.clikt.core.subcommands
import kotlinx.coroutines.runBlocking
import org.slf4j.simple.SimpleLogger

const val rootProjectName = "thesis"

fun main(args: Array<String>) = runBlocking {
    // System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info")
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "warn")

    if (OS.contains("mac", true)) System.setProperty("java.io.tmpdir", "/tmp")

    Logger.coroutineScope = this

    try {
        CliKt()
            .subcommands(
                CliP2P().subcommands(CliKafka(), CliRabbitMQ()),
                CliPubSub().subcommands(CliKafka(), CliRabbitMQ()),
                CliStream().subcommands(CliKafka(), CliRabbitMQ()),
            )
            .main(args)
    } finally {
        Logger.stopJob()
        println()
    }
}
