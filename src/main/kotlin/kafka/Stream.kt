package kafka

import IsoDateTime
import kotlinx.coroutines.CoroutineScope
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import kotlin.time.Duration

object Stream {
    suspend fun consume(
        coroutineScope: CoroutineScope,
        topic: String
    ) = PubSub.consume(
        coroutineScope,
        topic
    )

    suspend fun produce(
        coroutineScope: CoroutineScope,
        topic: String,
        key: String?,
        messageLambda: () -> String?,
        delay: Duration
    ) = PubSub.produce(
        coroutineScope,
        topic,
        key,
        messageLambda,
        delay
    )

    fun processing(
        inputTopic: String,
        outputTopic: String,
    ): KafkaStreams {
        val builder = StreamsBuilder()

        val store = mutableSetOf<IsoDateTime>()
        builder
            .stream<String, String>(inputTopic)
            .mapValues { value -> IsoDateTime.parse(value) }
            .filter { _, value -> value != null && store.add(value) }
            .mapValues { value -> value.toString() }
            .to(outputTopic)

        val topology = builder.build()

        return KafkaStreams(topology, streamsConfig)
    }
}
