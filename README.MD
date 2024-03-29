# Kafka with OpenSearch (for of ElasticSearch) demo

## How to run all docker images
- type `docker-compose up`

## How to stop all docker images
- type `docker-compose down`

## How to install Kafka locally (no need for this demo, but could be used for getting topics in command line)
- Download Kafka [Kafka download Pge](https://kafka.apache.org/quickstart)
- Go to download directory and type `tar -xzf kafka_X.XX-X.X.X.tgz`

## How to use this app
- Go to page `localhost:8080` and create topic `wikimedia.recentchange`
- To read current topics (not from history) from command line type: `./kafka-3.7.0-src/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic wikimedia.recentchange`
- To read all topics (also from history) from command line type: `./kafka-3.7.0-src/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic wikimedia.recentchange --from-beginning`

## How to use conductor (Conductor is app from company Conductor founded by Stéphane Maarek - popular Udemy Instructor)
- Go to page `localhost:8080`
- Login is `admin@conduktor.io` password is `admin`

## Docker compose explanation
### Service: zookeeper
- Uses the Docker image `confluentinc/cp-zookeeper:7.3.0`. Its hostname is set as zookeeper, and the container name is also set as zookeeper. The service exposes port 2181 and sets several environment variables for config setup.
- Service: kafka
  - Similarly, uses the Docker image `confluentinc/cp-kafka:7.3.0` with the hostname and container name of kafka. It exposes multiple ports and has multiple environment variables for configuration. It is set to depend on zookeeper, meaning it will only run if the zookeeper service is running.
- Service: schema-registry
  - Runs image `confluentinc/cp-schema-registry:7.3.0` with hostname and container name as `schema-registry`. It has similar settings like the above services and depends on both zookeeper and kafka services.
### Service: postgresql
- It utilizes the Docker image `postgres:14` and it works with a postgres database `namedconduktor-console` and accessible through a username `conduktor` and password `change_me`.
### Service: conductor-console
- This service utilizes Docker image `conduktor/conduktor-console:1.21.0`, it also binds a local file `./platform-config.yml to /opt/conduktor/platform-config.yaml` within the container. The container is dependent on postgresql.
### Service: conduktor-monitoring
- The service uses image `conduktor/conduktor-console-cortex:1.21.0` for monitoring and sets up environment variables for operation.
- Port mapping syntax is `"host:container"` which means that the service is accessible by the host on the left-side port, but within the container it acts on the right-side port.
- environment keys define environment variables for the containers.
- And depends_on is used so the services startup depends on the startup of another.
- The volumes key is used to set up data persistence or file sharing with the host system.