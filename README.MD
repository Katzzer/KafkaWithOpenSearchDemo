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
- To read topics in Conduktor go to `localhost:8080`, select Topics and select topic `wikimedia.recentchange`

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

## More information about Kafka
### acks
- The acks setting in Apache Kafka deals with the number of acknowledgements the producer needs from the broker(s). This affects the durability of the messages.
- There are three possible values for acks:
  - acks=0: The producer won't wait for acknowledgement from the broker(s). In this case, there's a possibility of data loss.
  - acks=1: This is the default setting. The producer will wait for acknowledgement received from the leader. If the leader fails, there might be data loss.
  - acks=all or -1: The producer will wait for acknowledgement from the leader and all the replicas.
### Idempotent Producer
- The Idempotent Producer in Kafka is a feature introduced to ensure that messages are delivered exactly once to a particular topic partition during the lifetime of the producer without any duplicates. This becomes a necessity in situations where network requests can fail, be duplicated, or responses lost.
- Setting in Java:
```java
Properties props = new Properties();
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```
- Since Kafka 3.0 is default used Idempotent Producer
### Retries
- Number of retries when Kafka reject message,
  - In Kafka <= 2.0 is default 0
  - In Kafka >= 2.1 is default 2147483647
### Setting by version
- Since Kafka 3.0 `acks=all (-1)` and `enable.indempotence=true`
- Since Kafka 2.8 and lower `acks=1` and `enable.indempotence=false`

## How to use Kafka in command line
- Go to kafka directory `cd kafka_2.13-3.1.0`
- Start ZooKeeper server `./bin/zookeeper-server-start.sh config/zookeeper.properties`
- Open new terminal and start Kafka broker service `./bin/kafka-server-start.sh config/server.properties`
- To get all messages open new terminal and type `./bin/kafka-console-consumer.sh --topic amigoscode --from-beginning --bootstrap-server localhost:9092`