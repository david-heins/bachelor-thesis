package kafka

import Logger
import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object PubSub {
    var ready = false

    suspend fun consume(
        coroutineScope: CoroutineScope,
        topic: String,
    ) = KafkaConsumer<String, String>(consumerConfig).use { consumer: Consumer<String, String> ->
        consumer.subscribe(setOf(topic))
        consumer.enforceRebalance()

        while (consumer.assignment().isEmpty()) {
            consumer.poll(1.seconds.toJavaDuration())
        }
        ready = true

        var isEmpty = false
        while (coroutineScope.isActive || !isEmpty) {
            val records = withContext(Dispatchers.IO) {
                consumer.poll(1.seconds.toJavaDuration())
            }
            isEmpty = records.isEmpty
            for (record in records.filterNotNull()) {
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
            }
        }
    }

    suspend fun produce(
        coroutineScope: CoroutineScope,
        topic: String,
        key: String?,
        messageLambda: () -> String?,
        delay: Duration
    ) = KafkaProducer<String, String>(producerConfig).use { producer: Producer<String, String> ->
        var firstIter = true
        while (coroutineScope.isActive) {
            if (firstIter) firstIter = false
            else delay(delay)

            messageLambda()?.let { message ->
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
