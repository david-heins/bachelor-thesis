import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.rabbitmq.client.BuiltinExchangeType
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CliKt : CliktCommand(name = rootProjectName) {
    private val config by findOrSetObject { Config }
    private val debug by option().flag()
    private val log by option()

    override fun run() {
        config.general = GeneralConfig(
            debug,
            log
        )

        Logger.init()
        Logger.startJob()
    }
}

class CliP2P : CliktCommand(name = "p2p", help = "Point-to-Point") {
    private val config by requireObject<Config>()
    private val mode by option().enum<Mode>(ignoreCase = true) { it.name.lowercase() }.default(Mode.All)
    private val amount by option().int().default(1).validate { it > 0 }
    private val validate by option().flag()

    override fun run() {
        config.pattern = Pattern.P2P

        val (consume, produce) = if (validate) {
            Pair(true, true)
        } else when (mode) {
            Mode.All -> Pair(true, true)
            Mode.Consume -> Pair(true, false)
            Mode.Produce -> Pair(false, true)
        }

        config.p2p = P2PConfig(
            consume,
            produce,
            amount,
            validate
        )
    }
}

class CliPubSub : CliktCommand(name = "pubsub", help = "Publish-Subscribe") {
    private val config by requireObject<Config>()
    private val consumerCount by option().int().default(2).validate { it in 1..CPU }
    private val producerCount by option().int().default(1).validate { it in 1..CPU }
    private val durationSec by option().int().default(30).validate { it > 30 }

    override fun run() {
        config.pattern = Pattern.PubSub
        config.pubsub = PubSubConfig(
            consumerCount,
            producerCount,
            durationSec.seconds
        )
    }
}

class CliStream : CliktCommand(name = "stream", help = "Stream") {
    private val config by requireObject<Config>()
    private val durationSec by option().int().default(30).validate { it > 30 }

    override fun run() {
        config.pattern = Pattern.Stream
        config.stream = StreamConfig(
            durationSec.seconds
        )
    }
}

class CliKafka : CliktCommand(name = "kafka", help = "Apache Kafka") {
    private val config by requireObject<Config>()
    private val host by option().default("localhost")
    private val port by option().int().default(9092)
    private val deleteConsumerGroup by option().flag()
    private val deleteConsumerGroupOffsets by option().flag()
    private val deleteTopic by option().flag()
    private val deleteStreamTopic by option().flag()
    private val topicName by option()
    private val inputStreamTopicName by option()
    private val outputStreamTopicName by option()
    private val key by option()
    private val consumerGroupId by option()
    private val streamApplicationId by option()
    private val delayMs by option().long().default(100).validate { it >= 100 }

    override fun run() {
        val defaultConsumerGroupId =
            if (config.pattern == Pattern.Stream) "default-stream-group"
            else "default"
        val defaultStreamApplicationId = "default-stream-app"
        val defaultTopic = "default"
        val defaultInputStreamTopic = "default-stream-input"
        val defaultOutputStreamTopic = "default-stream-output"

        config.middleware = Middleware.Kafka
        config.kafka = KafkaConfig(
            host,
            port,
            deleteConsumerGroup,
            deleteConsumerGroupOffsets,
            deleteTopic,
            deleteStreamTopic,
            consumerGroupId ?: defaultConsumerGroupId,
            topicName ?: defaultTopic,
            key,
            inputStreamTopicName ?: defaultInputStreamTopic,
            outputStreamTopicName ?: defaultOutputStreamTopic,
            streamApplicationId ?: defaultStreamApplicationId,
            delayMs.milliseconds
        )
        kafka.main()
    }
}

class CliRabbitMQ : CliktCommand(name = "rabbitmq", help = "RabbitMQ") {
    private val config by requireObject<Config>()
    private val username by option()
    private val password by option()
    private val virtualHost by option()
    private val host by option()
    private val port by option().int()
    private val deleteExchange by option().flag()
    private val deleteQueue by option().flag()
    private val clearQueue by option().flag()
    private val exchange by object : OptionGroup() {
        val exchangeName by option().required()
        val exchangeType by option().enum<BuiltinExchangeType>(ignoreCase = true) { it.name.lowercase() }
    }.cooccurring()
    private val queue by object : OptionGroup() {
        val queueName by option().required()
        val queueRoutingKey by option()
    }.cooccurring()
    private val messageRoutingKey by option()
    private val stream by object : OptionGroup() {
        val inputStreamQueueName by option().required()
        val outputStreamQueueName by option().required()
    }.cooccurring()
    private val delayMs by option().long().default(100).validate { it >= 100 }

    override fun run() {
        val defaultQueue = "default"
        val defaultInputStreamQueue = "default-stream-input"
        val defaultOutputStreamQueue = "default-stream-output"

        config.middleware = Middleware.RabbitMQ
        config.rabbitmq = RabbitMQConfig(
            username,
            password,
            virtualHost,
            host,
            port,
            deleteExchange,
            deleteQueue,
            clearQueue,
            exchange?.exchangeName ?: "",
            exchange?.exchangeType ?: BuiltinExchangeType.TOPIC,
            queue?.queueName ?: defaultQueue,
            queue?.queueRoutingKey ?: queue?.queueName ?: defaultQueue,
            messageRoutingKey ?: queue?.queueName ?: defaultQueue,
            stream?.inputStreamQueueName ?: defaultInputStreamQueue,
            stream?.outputStreamQueueName ?: defaultOutputStreamQueue,
            delayMs.milliseconds
        )
        rabbitmq.main()
    }
}
