#!/usr/bin/env bash

if command -v autossh > /dev/null
then
	echo "autossh was found and is being used"
	SSH="autossh -M 0"
else
	echo "autossh was not found, using ssh instead"
	SSH="ssh"
fi

${SSH} \
	-L localhost:3000:localhost:3000 \
	-L localhost:5551:localhost:5551 \
	-L localhost:5552:localhost:5552 \
	-L localhost:5671:localhost:5671 \
	-L localhost:5672:localhost:5672 \
	-L localhost:8801:localhost:8801 \
	-L localhost:9090:localhost:9090 \
	-L localhost:9092:localhost:9092 \
	-L localhost:9093:localhost:9093 \
	-L localhost:9308:localhost:9308 \
	-L localhost:15671:localhost:15671 \
	-L localhost:15672:localhost:15672 \
	-L localhost:15691:localhost:15691 \
	-L localhost:15692:localhost:15692 \
	"${@}"
