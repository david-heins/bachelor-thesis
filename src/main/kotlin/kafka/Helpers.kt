package kafka

import Config
import org.apache.kafka.clients.admin.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import java.util.*

internal val adminClientConfig: Map<String, Any> = mapOf(
    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to "${Config.kafka.host}:${Config.kafka.port}"
)
internal val consumerConfig: Map<String, Any> = mapOf(
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to ConsumerConfig.DEFAULT_MAX_POLL_RECORDS,
    ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG to false,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "${Config.kafka.host}:${Config.kafka.port}",
    ConsumerConfig.GROUP_ID_CONFIG to Config.kafka.consumerGroupId,
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
)
internal val producerConfig: Map<String, Any> = mapOf(
    ProducerConfig.ACKS_CONFIG to "all",
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "${Config.kafka.host}:${Config.kafka.port}",
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
)
internal val streamsConfig = StreamsConfig(
    mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to Config.kafka.streamApplicationId,
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "${Config.kafka.host}:${Config.kafka.port}",
        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to StringSerde::class.java,
        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to StringSerde::class.java
    )
)

fun createTopics(admin: Admin, topics: Set<String>) {
    val newTopics = topics.map {
        NewTopic(it, Optional.empty(), Optional.empty())
    }
    admin.createTopics(newTopics)
        .all()
        .get()
}

fun createTopic(admin: Admin, topic: String) =
    createTopics(admin, setOf(topic))

fun deleteConsumerGroups(admin: Admin, groupIds: Set<String>) {
    try {
        admin.deleteConsumerGroups(
            groupIds,
            DeleteConsumerGroupsOptions().timeoutMs(5000)
        )
            .all()
            .get()
    } catch (_: Exception) {
    }
}

fun deleteConsumerGroup(admin: Admin, groupId: String) =
    deleteConsumerGroups(admin, setOf(groupId))

fun deleteConsumerGroupOffsets(admin: Admin, groupId: String, topic: String) {
    val topicDescription = describeTopic(admin, topic) ?: throw Exception()
    val partitions = topicDescription
        .partitions()
        .map { TopicPartition(topic, it.partition()) }
        .toSet()
    try {
        admin.deleteConsumerGroupOffsets(
            groupId,
            partitions,
            DeleteConsumerGroupOffsetsOptions().timeoutMs(5000)
        )
            .all()
            .get()
    } catch (_: Exception) {
    }
}

fun deleteTopics(admin: Admin, topics: Set<String>) {
    admin.deleteTopics(topics)
        .all()
        .get()
}

fun deleteTopic(admin: Admin, topic: String) =
    deleteTopics(admin, setOf(topic))

fun describeTopics(admin: Admin, topics: Set<String>): Map<String, TopicDescription> {
    return try {
        admin
            .describeTopics(topics)
            .allTopicNames()
            .get()
    } catch (exception: Exception) {
        emptyMap()
    }
}

fun describeTopic(admin: Admin, topic: String): TopicDescription? =
    describeTopics(admin, setOf(topic)).getOrDefault(topic, null)
