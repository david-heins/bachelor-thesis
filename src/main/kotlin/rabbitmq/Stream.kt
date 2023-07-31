package rabbitmq

import IsoDateTime
import com.rabbitmq.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Stream {
    suspend fun consume(
        coroutineScope: CoroutineScope,
        connection: Connection,
        queue: String
    ) = PubSub.consume(
        coroutineScope,
        connection,
        queue
    )

    suspend fun produce(
        coroutineScope: CoroutineScope,
        connection: Connection,
        exchange: String,
        routingKey: String,
        messageLambda: () -> String?,
        delay: Duration
    ) = PubSub.produce(
        coroutineScope,
        connection,
        exchange,
        routingKey,
        messageLambda,
        delay
    )

    private class MyProcessor(channel: Channel, val queue: String) :
        DefaultConsumer(channel) {
        private val store = mutableSetOf<IsoDateTime>()
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope?,
            properties: AMQP.BasicProperties,
            body: ByteArray?
        ) {
            if (envelope == null || body == null) return

            val deliveryTag = envelope.deliveryTag
            val message = body.decodeToString()

            channel.basicAck(deliveryTag, false)

            val value = IsoDateTime.parse(message)
            if (value == null || !store.add(value)) return

            channel.basicPublish(
                "",
                queue,
                messageProps,
                value.toString().encodeToByteArray()
            )
        }
    }

    suspend fun processing(
        coroutineScope: CoroutineScope,
        connection: Connection,
        inputQueue: String,
        outputQueue: String
    ) = connection.createChannel().use { channel ->
        val processor = MyProcessor(channel, outputQueue)

        channel.basicQos(prefetchCount)
        channel.basicConsume(inputQueue, false, processor)

        while (coroutineScope.isActive) {
            delay(1.seconds)
        }

        channel.basicCancel(processor.consumerTag)
    }
}
