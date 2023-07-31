import Validator.addCount
import Validator.removeCount
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object Logger {
    lateinit var sendChannel: SendChannel<Pair<Type, Message>>
    private lateinit var receiveChannel: ReceiveChannel<Pair<Type, Message>>

    var coroutineScope: CoroutineScope? = null
    private var logFile: File? = null
    private var loggingJob: Job? = null

    enum class Type {
        Consumer, Info, Producer
    }

    sealed class Message {
        data class Single(val key: String?, val value: String) : Message()
        data class Multiple(val messages: List<Single>) : Message()
    }

    fun init() {
        val file = File(Config.general.log ?: "")
        try {
            if (file.parentFile.isDirectory) {
                if (file.isFile || file.createNewFile()) {
                    logFile = file
                }
            }
        } catch (_: Exception) {
        }

        if (logFile == null) {
            logFile = File("${System.getProperty("java.io.tmpdir")}${File.separator}${rootProjectName}.log")
        }
    }

    fun startJob() {
        val coroutineScope = coroutineScope ?: throw Exception()

        if (loggingJob == null) {
            Channel<Pair<Type, Message>>(
                capacity = Channel.BUFFERED
            ).let { channel ->
                sendChannel = channel
                receiveChannel = channel
            }
            loggingJob = coroutineScope.launch(Dispatchers.Default) { log() }
        }
    }

    fun stopJob() {
        val loggingJob = loggingJob ?: throw Exception()

        sendChannel.close()
        runBlocking { withTimeout(3000L) { loggingJob.join() } }
        if (loggingJob.isActive) loggingJob.cancel()
    }

    private suspend fun log() {
        val logFile = logFile ?: throw Exception()

        withContext(Dispatchers.IO) {
            logFile.writeBytes(byteArrayOf())
            BufferedWriter(
                FileWriter(
                    logFile,
                    Charsets.UTF_8,
                    true
                )
            ).use { file ->
                do {
                    val result = receiveChannel.receiveCatching()
                    if (Config.general.debug) println(result)
                    result.getOrNull()?.let { (type, message) ->
                        when (message) {
                            is Message.Single -> message.let { (key, value) ->
                                file.append("${type}: \"${key}\" - \"${value}\"\n")
                            }

                            is Message.Multiple -> message.messages.forEach { (key, value) ->
                                file.append("${type}: \"${key}\" - \"${value}\"\n")
                            }
                        }
                    }
                } while (!result.isClosed)
            }
        }
    }

    suspend fun logSummary(amount: Int) {
        if (addCount == amount && removeCount == amount) {
            sendChannel.send(
                Pair(
                    Type.Info,
                    Message.Single(
                        null,
                        "VALID: Validator counts (${addCount} | ${removeCount}) match requested amount (${amount})"
                    )
                )
            )
        } else {
            sendChannel.send(
                Pair(
                    Type.Info,
                    Message.Single(
                        null,
                        "INVALID: Validator counts (${addCount} | ${removeCount}) don't match requested amount (${amount})"
                    )
                )
            )
        }
    }
}
