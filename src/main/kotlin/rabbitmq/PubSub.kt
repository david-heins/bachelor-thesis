package rabbitmq

import Logger
import com.rabbitmq.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object PubSub {
    private class MyConsumer(channel: Channel, val coroutineScope: CoroutineScope) :
        DefaultConsumer(channel) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope?,
            properties: AMQP.BasicProperties,
            body: ByteArray?
        ) {
            if (envelope == null || body == null) return

            val deliveryTag = envelope.deliveryTag
            val routingKey = envelope.routingKey
            val message = body.decodeToString()

            channel.basicAck(deliveryTag, false)
            coroutineScope.launch {
                Logger.sendChannel.send(
                    Pair(
                        Logger.Type.Consumer,
                        Logger.Message.Single(routingKey, message)
                    )
                )
            }
        }
    }

    suspend fun consume(
        coroutineScope: CoroutineScope,
        connection: Connection,
        queue: String
    ) = connection.createChannel().use { channel ->
        val consumer = MyConsumer(channel, coroutineScope)

        channel.basicQos(prefetchCount)
        channel.basicConsume(queue, false, consumer)

        while (coroutineScope.isActive) {
            delay(1.seconds)
        }

        channel.basicCancel(consumer.consumerTag)
    }

    suspend fun produce(
        coroutineScope: CoroutineScope,
        connection: Connection,
        exchange: String,
        routingKey: String,
        messageLambda: () -> String?,
        delay: Duration
    ) = connection.createChannel().use { channel ->
        var firstIter = true
        while (coroutineScope.isActive) {
            if (firstIter) firstIter = false
            else delay(delay)

            messageLambda()?.let { message ->
                channel.basicPublish(
                    exchange,
                    routingKey,
                    messageProps,
                    message.encodeToByteArray()
                )
                Logger.sendChannel.send(
                    Pair(
                        Logger.Type.Producer,
                        Logger.Message.Single(routingKey, message)
                    )
                )
            }
        }
    }
}
