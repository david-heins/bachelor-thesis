package rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Connection
import com.rabbitmq.client.MessageProperties

internal const val prefetchCount = 500

internal val messageProps: AMQP.BasicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
internal val queueArgs = mapOf(
    "x-queue-type" to "quorum"
)
internal val streamArgs = mapOf(
    "x-queue-type" to "stream"
)

fun clearQueue(connection: Connection, queue: String) {
    connection.createChannel().use { channel ->
        channel.queuePurge(queue)
    }
}

fun declareExchange(connection: Connection, exchange: String, type: BuiltinExchangeType) {
    connection.createChannel().use { channel ->
        channel.exchangeDeclare(exchange, type, true, false, null)
    }
}

fun declareQueue(connection: Connection, queue: String) {
    connection.createChannel().use { channel ->
        channel.queueDeclare(queue, true, false, false, queueArgs)
    }
}

fun declareStream(connection: Connection, stream: String) {
    connection.createChannel().use { channel ->
        channel.queueDeclare(stream, true, false, false, streamArgs)
    }
}

fun deleteExchange(connection: Connection, exchange: String) {
    connection.createChannel().use { channel ->
        channel.exchangeDelete(exchange, false)
    }
}

fun deleteQueue(connection: Connection, queue: String) {
    connection.createChannel().use { channel ->
        channel.queueDelete(queue, false, false)
    }
}

fun bindQueue(connection: Connection, queue: String, exchange: String, routingKey: String, unbind: Boolean = false) {
    connection.createChannel().use { channel ->
        if (unbind) {
            channel.queueUnbind(queue, exchange, routingKey)
        } else {
            channel.queueBind(queue, exchange, routingKey)
        }
    }
}
