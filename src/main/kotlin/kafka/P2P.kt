package kafka

import Logger
import Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import retryGeneratorUntilConditionOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object P2P {
    var ready = false

    suspend fun consume(
        topic: String?,
        amount: Int,
        validate: Boolean
    ) = KafkaConsumer<String, String>(consumerConfig).use { consumer: Consumer<String, String> ->
        consumer.subscribe(setOf(topic))
        consumer.enforceRebalance()

        while (consumer.assignment().isEmpty()) {
            consumer.poll(1.seconds.toJavaDuration())
        }
        ready = true

        var count = 0
        var isEmpty = false
        while (count < amount || !isEmpty) {
            val records = withContext(Dispatchers.IO) {
                consumer.poll(1.seconds.toJavaDuration())
            }
            isEmpty = records.isEmpty
            for (record in records.filterNotNull()) {
                if (count >= amount) break

                if (validate && !Validator.validate(record.value(), true)) continue

                val key = record.key()
                val message = record.value()
                val offset = record.offset()
                val partition = record.partition()

                val topicPartition = TopicPartition(topic, partition)
                val offsetAndMetadata = OffsetAndMetadata(offset + 1)
                consumer.commitSync(
                    mapOf(topicPartition to offsetAndMetadata)
                )
                Logger.sendChannel.send(
                    Pair(
                        Logger.Type.Consumer,
                        Logger.Message.Single(key, message)
                    )
                )
                count++
            }
        }
    }

    suspend fun produce(
        topic: String,
        key: String?,
        messageLambda: () -> String?,
        delay: Duration,
        amount: Int,
        validate: Boolean
    ) = KafkaProducer<String, String>(producerConfig).use { producer: Producer<String, String> ->
        for (index in 1..amount) {
            if (index > 1) delay(delay)
            if (validate) {
                retryGeneratorUntilConditionOrNull(3.seconds, messageLambda) {
                    Validator.validate(it, false)
                }
            } else {
                messageLambda()
            }?.let { message ->
                val record = ProducerRecord(topic, key, message)
                producer.send(record)
                Logger.sendChannel.send(
                    Pair(
                        Logger.Type.Producer,
                        Logger.Message.Single(key, message)
                    )
                )
            }
        }
    }
}
