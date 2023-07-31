package rabbitmq

import Logger
import Validator
import com.rabbitmq.client.Connection
import kotlinx.coroutines.delay
import retryGeneratorUntilConditionOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object P2P {
    suspend fun consume(
        connection: Connection,
        queue: String,
        delay: Duration,
        amount: Int,
        validate: Boolean
    ) = connection.createChannel().use { channel ->
        var firstIter = true
        var count = 0
        var isEmpty = false
        while (count < amount || !isEmpty) {
            if (firstIter) firstIter = false
            else delay(delay)

            val response = channel.basicGet(queue, false)

            isEmpty = response == null || response.body == null

            if (validate && !Validator.validate(response?.body?.decodeToString(), true)) continue

            val deliveryTag = response.envelope.deliveryTag
            val routingKey = response.envelope.routingKey
            val message = response.body.decodeToString()


            channel.basicAck(deliveryTag, false)
            Logger.sendChannel.send(
                Pair(
                    Logger.Type.Consumer,
                    Logger.Message.Single(routingKey, message)
                )
            )
            count++
        }
    }

    suspend fun produce(
        connection: Connection,
        exchange: String,
        routingKey: String,
        messageLambda: () -> String?,
        delay: Duration,
        amount: Int,
        validate: Boolean
    ) = connection.createChannel().use { channel ->
        for (index in 1..amount) {
            if (index > 1) delay(delay)
            if (validate) {
                retryGeneratorUntilConditionOrNull(3.seconds, messageLambda) {
                    Validator.validate(it, false)
                }
            } else {
                messageLambda()
            }?.let { message ->
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
