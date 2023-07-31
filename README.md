# Bachelor Thesis Code

_**Note**: This code was only tested on Linux_

## Prerequisite

- [Docker](https://docker.com/get-started/) (_not_ Docker Desktop)
- [Java Development Kit (JDK) 17 (LTS)](https://whichjdk.com/)
  (recommended: [Azul Zulu](https://azul.com/downloads/)
  or [Adoptium Eclipse Temurin](https://adoptium.net/temurin/releases/))

## Getting Started

- Create & start services (detached)

```shell
docker compose up -d
```

- List containers & compose services

```shell
docker ps -a
docker compose ls -a
```

- Stop & remove services

```shell
docker compose down
```

- Build app

```shell
./gradlew clean
./gradlew build
```

- Run app

```shell
./gradlew installDist
./build/install/thesis/bin/thesis -h
```
