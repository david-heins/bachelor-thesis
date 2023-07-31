package kafka

import Config
import Logger
import Middleware
import Pattern
import getIsoDateTimeNow
import kotlinx.coroutines.*
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClient
import throwConfigMiddleware
import throwConfigPattern
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = runBlocking {
    if (Config.middleware != Middleware.Kafka) {
        throwConfigMiddleware(expected = Middleware.Kafka, provided = Config.middleware)
    }

    AdminClient.create(adminClientConfig).use { admin: Admin ->
        var returning = false
        if (Config.kafka.deleteConsumerGroup) {
            deleteConsumerGroup(
                admin,
                Config.kafka.consumerGroupId
            )
            returning = true
        }
        if (Config.kafka.deleteConsumerGroupOffsets) {
            deleteConsumerGroupOffsets(
                admin,
                Config.kafka.consumerGroupId,
                Config.kafka.topic
            )
            returning = true
        }
        if (Config.kafka.deleteTopic) {
            deleteTopic(
                admin,
                Config.kafka.topic
            )
            returning = true
        }
        if (Config.kafka.deleteStreamTopic) {
            deleteTopic(
                admin,
                Config.kafka.inputStreamTopic
            )
            deleteTopic(
                admin,
                Config.kafka.outputStreamTopic
            )
            returning = true
        }
        if (returning) return@use

        if (Config.pattern == Pattern.Stream) {
            setOf(
                Config.kafka.inputStreamTopic,
                Config.kafka.outputStreamTopic
            )
        } else {
            setOf(Config.kafka.topic)
        }.forEach {
            if (describeTopic(admin, it) == null) {
                createTopic(
                    admin,
                    it
                )
            }
        }

        if (Config.pattern == Pattern.P2P && Config.p2p.validate) {
            deleteConsumerGroupOffsets(
                admin,
                Config.kafka.consumerGroupId,
                Config.kafka.topic
            )
        }
    }

    when (Config.pattern) {
        Pattern.P2P -> startP2P()
        Pattern.PubSub -> startPubSub()
        Pattern.Stream -> startStream()
        else -> throwConfigPattern(
            expected = Pattern.entries.toTypedArray(),
            provided = Config.pattern
        )
    }
}

suspend fun startP2P() = coroutineScope {
    val consumerJob = launch(Dispatchers.Default) {
        if (Config.p2p.consume) {
            P2P.consume(
                Config.kafka.topic,
                Config.p2p.amount,
                Config.p2p.validate
            )
        }
    }

    while (!P2P.ready) {
        delay(1.seconds)
    }

    val producerJob = launch(Dispatchers.Default) {
        if (Config.p2p.produce) {
            P2P.produce(
                Config.kafka.topic,
                Config.kafka.key,
                getIsoDateTimeNow,
                Config.kafka.delay,
                Config.p2p.amount,
                Config.p2p.validate
            )
        }
    }

    producerJob.join()
    consumerJob.join()

    if (Config.p2p.validate) Logger.logSummary(Config.p2p.amount)
}

suspend fun startPubSub() = coroutineScope {
    val consumerJobs = List(Config.pubsub.consumerCount) {
        launch(Dispatchers.Default) {
            PubSub.consume(
                this,
                Config.kafka.topic
            )
        }
    }

    while (!PubSub.ready) {
        delay(1.seconds)
    }

    val producerJobs = List(Config.pubsub.producerCount) {
        launch(Dispatchers.Default) {
            PubSub.produce(
                this,
                Config.kafka.topic,
                Config.kafka.key,
                getIsoDateTimeNow,
                Config.kafka.delay
            )
        }
    }

    delay(Config.pubsub.duration)

    producerJobs.forEach { it.cancelAndJoin() }
    consumerJobs.forEach { it.cancelAndJoin() }
}

suspend fun startStream() = coroutineScope {
    val consumerJob = launch(Dispatchers.Default) {
        Stream.consume(
            this,
            Config.kafka.outputStreamTopic
        )
    }

    while (!PubSub.ready) {
        delay(1.seconds)
    }

    val processor = Stream.processing(
        Config.kafka.inputStreamTopic,
        Config.kafka.outputStreamTopic
    )

    processor.start()

    val producerJob = launch(Dispatchers.Default) {
        Stream.produce(
            this,
            Config.kafka.inputStreamTopic,
            Config.kafka.key,
            getIsoDateTimeNow,
            Config.kafka.delay
        )
    }

    delay(Config.stream.duration)

    producerJob.cancelAndJoin()

    processor.close()
    processor.cleanUp()

    consumerJob.cancelAndJoin()
}
