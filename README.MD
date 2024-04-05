# Kafka with OpenSearch (for of ElasticSearch) demo

## Description
- This is simple application with 2 Spring Boot modules and OpenSearch
  - producer (Spring Boot App) get data from `https://stream.wikimedia.org/v2/stream/recentchange` and store it in Kafka
  - consumer (Spring Boot App) get data from Kafka and send it to OpenSearch
  - OpenSearch stores data which can be searched or visualized using OpenSearch Dashboard, or further transformed by Logstash.

### Services description
- Conduktor - runs on port `localhost:8080` - show data from Kafka in GUI
- OpenSearch database - runs on port `localhost:9200`
- OpenSearch dashboard - runs on port `localhost:5601` - GUI for OpenSearch - for searching and visualization

## How to run all docker images
- type `docker-compose up` in `kafka-producer-wikimedia`
- type `docker-compose up` in `kafka-consumer-opensearch`

## How to stop all docker images
- type `docker-compose down` in directory `kafka-producer-wikimedia` and `kafka-consumer-opensearch`

## How to use this app
- Run producer from module `kafka-producer-wikimedia`
- Go to Conductor on page `localhost:8080` and create topic `wikimedia.recentchange`
- To read current topics (not from history) from command line type: `./kafka-3.7.0-src/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic wikimedia.recentchange`
- To read all topics (also from history) from command line type: `./kafka-3.7.0-src/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic wikimedia.recentchange --from-beginning`
- To read topics in Conduktor go to `localhost:8080`, select Topics and select topic `wikimedia.recentchange`

## How to use conductor (Conductor is app from company Conductor founded by Stéphane Maarek - popular Udemy Instructor)
- Go to page `localhost:8080`
- Login is `admin@conduktor.io` password is `admin`
- Select cluster (should be preselected `my-local-kafka-cluster`)
- In `Topics` is list of Topics and inside all messages
- In `Consumer Groups` - is list of Consumer Groups where is possible to see and set offset (In Kafka, an `offset` is a unique ID for a record in a partition. Consumers use offsets to track their position in a partition and resume from their last position in case of a failure.)

## How to use OpenSearch
- Go to `localhost:5601`
### Search example
- Get all documents that ends with `.jpg` in `Dev Tools` in `Console`:
```json
GET /wikimedia/_search
{
  "query": {
    "query_string" : {
      "default_field" : "title_url",
      "query" : "*.jpg"
    }
  }
}
```
- Get all documents:
```json
GET /wikimedia/_search
{
  "query": {
    "match_all": {}
  }
}
```
- Get single document by id (you have to change id):
```json
GET /wikimedia/_doc/t9sHjo4BIjiASG2HF4xv
```
### Dejavu
- Dejavu is a modern, open-source, web-based interface for Elasticsearch that comes with powerful features like filter views, query views, configurable CORS, a user-friendly data importer, and more. It is developed by Appbase.io and can serve as a nice GUI for interacting with Elasticsearch clusters.
- Dejavu is primarily available in four variants:
  - Chrome extension
  - Docker: `docker run -p 1358:1358 -d appbaseio/dejavu`. Then Dejavu can be accessed using http://localhost:1358.`
  - Standalone app
  - Hosted app

## Free online OpenSearch
- Go to [bonsai.io](https://bonsai.io)
- There is free tier with Kibana and limitation:
  - SSD capacity: 125MB
  - Memory Limit: 125MB

## Docker compose explanation
### Service: zookeeper
- Uses the Docker image `confluentinc/cp-zookeeper:7.3.0`. Its hostname is set as `zookeeper`, and the container name is also set as `zookeeper`. The service exposes port 2181 and sets several environment variables for config setup.
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
### Zookeeper
- In the context of Apache Kafka, ZooKeeper is used for:
  - Maintaining Cluster Membership: ZooKeeper keeps track of the status of Kafka nodes in the cluster and keeps track of Kafka topics, partitions, etc.
  - Failover Handling: In the case of a Kafka broker failure, ZooKeeper will notify the producer and consumer about the broker failure and subsequent recovery.
  - Configuration and Synchronization: Kafka uses Zookeeper to manage and synchronize topic configurations. ZooKeeper facilitates communication between broker and client, making sure changes propagate across the cluster uniformly.
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
- Current application setting is visible when app starts
### Kafka connectors
- Kafka Connectors are some sort of configuration for connection to external systems, but they also encapsulate more complex functionality
- Kafka Sink Connector - simple explanation Kafka push data to external system like sql database
  - However, this is not technically correct, while it can seem like Kafka is "pushing" the data into another system when you're using a Sink Connector, behind the scenes it's actually the Sink Connector that is pulling the data from Kafka, then converting and loading it into the destination system.
  - So instead of making Java application that will pull data from Kafka and push it to another system we use Kafka sink
### Kafka Connect REST API
- All the actions performed by Landoop Kafka Connect UI are actually triggering REST API calls to Kafka Connect
- REST API examples:
  - Get Worker information
  - List Connectors available on a Worker
  - Ask about Active Connectors
  - Get Information about a Connector Tasks and Config
  - Get Connector Status
  - Pause / Resume a Connector
  - Delete our Connector
  - Create a new Connector
  - Update Connector configuration
  - Get Connector configuration
- More information: [Kafka Connect REST API Confluent documentation](https://docs.confluent.io/platform/current/connect/monitoring.html)
  - example `curl localhost:8083/connectors` - List active connectors on a worker:
### Landoop application
- Landoop, now known as Lenses.io, provides a Stream Processing platform for managing Apache Kafka clusters and other streaming data technologies.

## How to use Kafka in command line (no need for this demo, but could be used for getting topics in command line)
- Download Kafka [Kafka download Page](https://kafka.apache.org/quickstart)
- Go to download directory and type `tar -xzf kafka_X.XX-X.X.X.tgz`
- Go to kafka directory `cd kafka_X.XX-X.X.X`
- Start ZooKeeper server `./bin/zookeeper-server-start.sh config/zookeeper.properties`
- Open new terminal and start Kafka broker service `./bin/kafka-server-start.sh config/server.properties`
- To get all messages open new terminal and type `./bin/kafka-console-consumer.sh --topic amigoscode --from-beginning --bootstrap-server localhost:9092`
