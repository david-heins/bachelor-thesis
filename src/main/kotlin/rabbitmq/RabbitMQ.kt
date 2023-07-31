package rabbitmq

import Config
import Logger
import Middleware
import Pattern
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import getIsoDateTimeNow
import kotlinx.coroutines.*
import throwConfigMiddleware
import throwConfigPattern

fun main(): Unit = runBlocking {
    if (Config.middleware != Middleware.RabbitMQ) {
        throwConfigMiddleware(expected = Middleware.RabbitMQ, provided = Config.middleware)
    }

    ConnectionFactory().apply {
        username = Config.rabbitmq.username ?: ConnectionFactory.DEFAULT_USER
        password = Config.rabbitmq.password ?: ConnectionFactory.DEFAULT_PASS
        virtualHost = Config.rabbitmq.virtualHost ?: ConnectionFactory.DEFAULT_VHOST
        host = Config.rabbitmq.host ?: ConnectionFactory.DEFAULT_HOST
        port = Config.rabbitmq.port ?: ConnectionFactory.USE_DEFAULT_PORT
    }
        .newConnection().use { connection ->
            var returning = false
            if (Config.rabbitmq.clearQueue) {
                clearQueue(connection, Config.rabbitmq.queue)
                returning = true
            }
            if (Config.rabbitmq.deleteQueue) {
                deleteQueue(connection, Config.rabbitmq.queue)
                returning = true
            }
            if (Config.rabbitmq.deleteExchange) {
                deleteExchange(connection, Config.rabbitmq.exchange)
                returning = true
            }
            if (returning) return@use

            if (Config.pattern != Pattern.Stream && Config.rabbitmq.exchange != "") {
                declareExchange(
                    connection,
                    Config.rabbitmq.exchange,
                    Config.rabbitmq.exchangeType
                )
            }

            if (Config.pattern == Pattern.Stream) {
                setOf(
                    Config.rabbitmq.inputStreamQueue,
                    Config.rabbitmq.outputStreamQueue
                ).forEach {
                    declareStream(connection, it)
                }
            } else {
                declareQueue(
                    connection,
                    Config.rabbitmq.queue
                )
            }

            if (Config.pattern != Pattern.Stream && Config.rabbitmq.exchange != "") {
                bindQueue(
                    connection,
                    Config.rabbitmq.queue,
                    Config.rabbitmq.exchange,
                    Config.rabbitmq.queueRoutingKey,
                    false
                )
            }

            if (Config.pattern == Pattern.P2P && Config.p2p.validate) {
                clearQueue(connection, Config.rabbitmq.queue)
            }

            when (Config.pattern) {
                Pattern.P2P -> startP2P(connection)
                Pattern.PubSub -> startPubSub(connection)
                Pattern.Stream -> startStream(connection)
                else -> throwConfigPattern(
                    expected = Pattern.entries.toTypedArray(),
                    provided = Config.pattern
                )
            }

            if (Config.pattern != Pattern.Stream && Config.rabbitmq.exchange != "") {
                bindQueue(
                    connection,
                    Config.rabbitmq.queue,
                    Config.rabbitmq.exchange,
                    Config.rabbitmq.queueRoutingKey,
                    true
                )
            }
        }
}

suspend fun startP2P(connection: Connection) = coroutineScope {
    val consumerJob = launch(Dispatchers.Default) {
        if (Config.p2p.consume) {
            P2P.consume(
                connection,
                Config.rabbitmq.queue,
                Config.rabbitmq.delay,
                Config.p2p.amount,
                Config.p2p.validate
            )
        }
    }

    val producerJob = launch(Dispatchers.Default) {
        if (Config.p2p.produce) {
            P2P.produce(
                connection,
                Config.rabbitmq.exchange,
                Config.rabbitmq.messageRoutingKey,
                getIsoDateTimeNow,
                Config.rabbitmq.delay,
                Config.p2p.amount,
                Config.p2p.validate
            )
        }
    }

    producerJob.join()
    consumerJob.join()

    if (Config.p2p.validate) Logger.logSummary(Config.p2p.amount)
}

suspend fun startPubSub(connection: Connection) = coroutineScope {
    val consumerJobs = List(Config.pubsub.consumerCount) {
        launch(Dispatchers.Default) {
            PubSub.consume(
                this,
                connection,
                Config.rabbitmq.queue
            )
        }
    }

    val producerJobs = List(Config.pubsub.producerCount) {
        launch(Dispatchers.Default) {
            PubSub.produce(
                this,
                connection,
                Config.rabbitmq.exchange,
                Config.rabbitmq.messageRoutingKey,
                getIsoDateTimeNow,
                Config.rabbitmq.delay
            )
        }
    }

    delay(Config.pubsub.duration)

    producerJobs.forEach { it.cancelAndJoin() }
    consumerJobs.forEach { it.cancelAndJoin() }
}

suspend fun startStream(connection: Connection) = coroutineScope {
    val consumerJob = launch(Dispatchers.Default) {
        PubSub.consume(
            this,
            connection,
            Config.rabbitmq.outputStreamQueue
        )
    }

    val processingJob = launch(Dispatchers.Default) {
        Stream.processing(
            this,
            connection,
            Config.rabbitmq.inputStreamQueue,
            Config.rabbitmq.outputStreamQueue
        )
    }

    val producerJob = launch(Dispatchers.Default) {
        PubSub.produce(
            this,
            connection,
            "",
            Config.rabbitmq.inputStreamQueue,
            getIsoDateTimeNow,
            Config.rabbitmq.delay
        )
    }

    delay(Config.stream.duration)

    producerJob.cancelAndJoin()
    processingJob.cancelAndJoin()
    consumerJob.cancelAndJoin()
}
