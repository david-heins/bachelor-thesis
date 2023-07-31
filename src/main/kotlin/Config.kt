import com.rabbitmq.client.BuiltinExchangeType
import kotlin.time.Duration

object Config {
    var middleware: Middleware? = null
    var pattern: Pattern? = null
    lateinit var general: GeneralConfig
    lateinit var p2p: P2PConfig
    lateinit var pubsub: PubSubConfig
    lateinit var stream: StreamConfig
    lateinit var kafka: KafkaConfig
    lateinit var rabbitmq: RabbitMQConfig
}

data class GeneralConfig(
    val debug: Boolean,
    val log: String?
)

data class P2PConfig(
    val consume: Boolean,
    val produce: Boolean,
    val amount: Int,
    val validate: Boolean
)

data class PubSubConfig(
    val consumerCount: Int,
    val producerCount: Int,
    val duration: Duration
)

data class StreamConfig(
    val duration: Duration
)

data class KafkaConfig(
    val host: String?,
    val port: Int?,
    val deleteConsumerGroup: Boolean,
    val deleteConsumerGroupOffsets: Boolean,
    val deleteTopic: Boolean,
    val deleteStreamTopic: Boolean,
    val consumerGroupId: String,
    val topic: String,
    val key: String?,
    val inputStreamTopic: String,
    val outputStreamTopic: String,
    val streamApplicationId: String,
    val delay: Duration
)

data class RabbitMQConfig(
    val username: String?,
    val password: String?,
    val virtualHost: String?,
    val host: String?,
    val port: Int?,
    val deleteExchange: Boolean,
    val deleteQueue: Boolean,
    val clearQueue: Boolean,
    val exchange: String,
    val exchangeType: BuiltinExchangeType = BuiltinExchangeType.DIRECT,
    val queue: String,
    val queueRoutingKey: String,
    val messageRoutingKey: String,
    val inputStreamQueue: String,
    val outputStreamQueue: String,
    val delay: Duration
)

enum class Mode {
    All, Consume, Produce
}

enum class Middleware {
    Kafka, RabbitMQ
}

enum class Pattern {
    P2P, PubSub, Stream
}
