#!/usr/bin/env bash

ARG1="$(echo "${1}" | tr '[:upper:]' '[:lower:]')"
ARG2="$(echo "${2}" | tr '[:upper:]' '[:lower:]')"

if test "$(docker info --format json | jq -r '(.Name | ascii_downcase | contains("docker-desktop")) or (.OperatingSystem | ascii_downcase | contains("docker desktop"))')" = "true"
then
	echo "Docker Desktop is not supported due to lack of support for host networking!"
	exit 1
fi

if test -z "${ARG2}"
then
	ARG2=1000000
fi

help() {
	echo "Invalid argument"
	echo ""
	echo "Usage: ${0} <kafka|rabbitmq|rabbitmq-stream> [message count = 1000000]"
	echo ""
	exit 1
}

kafka() {
	docker run -t --rm --network host --entrypoint /bin/kafka-topics confluentinc/cp-kafka:latest --bootstrap-server localhost:9092 --create --if-not-exists --topic perf-test
	time (
		docker run -t --rm --network host --entrypoint /bin/kafka-producer-perf-test confluentinc/cp-kafka:latest --record-size 32 --throughput -1 --topic perf-test --producer-props bootstrap.servers=localhost:9092 --num-records "${ARG2}"
		docker run -t --rm --network host --entrypoint /bin/kafka-consumer-perf-test confluentinc/cp-kafka:latest --bootstrap-server localhost:9092 --group perf-test --topic perf-test --reporting-interval 1000 --messages "${ARG2}"
	)
	docker run -t --rm --network host --entrypoint /bin/kafka-topics confluentinc/cp-kafka:latest --bootstrap-server localhost:9092 --delete --if-exists --topic perf-test
	exit 0
}

rabbitmq() {
	time docker run -t --rm --network host pivotalrabbitmq/perf-test:latest --quorum-queue --auto-delete true --autoack --predeclared --exchange "" --routing-key perf-test --queue perf-test --qos 0 --rate -1 --size 32 --uris amqp://localhost:5672 --cmessages "${ARG2}" --pmessages "${ARG2}"
	exit 0
}

rabbitmq_stream() {
	time docker run -t --rm --network host pivotalrabbitmq/stream-perf-test:latest --delete-streams --offset first --rate -1 --size 32 --streams perf-test-stream --uris rabbitmq-stream://localhost:5552 --time 60
	exit 0
}

case "${ARG1}" in
	kafka) kafka ;;
	rabbitmq) rabbitmq ;;
	rabbitmq-stream) rabbitmq_stream ;;
	*) help ;;
esac
