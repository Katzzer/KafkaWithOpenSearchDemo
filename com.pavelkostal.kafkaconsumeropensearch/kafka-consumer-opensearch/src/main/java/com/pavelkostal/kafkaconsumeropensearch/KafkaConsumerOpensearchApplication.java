package com.pavelkostal.kafkaconsumeropensearch;

import com.google.gson.JsonParser;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@SpringBootApplication
@Log4j2
public class KafkaConsumerOpensearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaConsumerOpensearchApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner() {
		return args -> {
			RestHighLevelClient openSearchClient = createOpenSearchClient();
			KafkaConsumer<String, String> consumer = createKafkaConsumer();

			setShutdownHook(consumer);

			try (openSearchClient; consumer) {
				createIndexInOpenSearch(openSearchClient);

				// subscribe the consumer to topic
				consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

				getDataFromKafkaAndSendItToOpenSearch(consumer, openSearchClient);

			} catch (WakeupException e) {
				log.info("Consumer is starting to shut down");
			} catch (Exception e) {
				log.error("Unexpected exception in the consumer", e);
			} finally {
				consumer.close(); // close the consumer, this will also commit offsets
				openSearchClient.close();
				log.info("The consumer is now gracefully shut down");
			}

		};
	}

	private static void createIndexInOpenSearch(RestHighLevelClient openSearchClient) throws IOException {
		boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);

		if (!indexExists) {
			CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
			openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT); // creates "wikimedia" index in OpenSearch
			log.info("Index wikimedia create");
		} else {
			log.info("Index wikimedia already exists");
		}
	}

	private static void getDataFromKafkaAndSendItToOpenSearch(KafkaConsumer<String, String> consumer, RestHighLevelClient openSearchClient) throws IOException {
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

			int recordCount = records.count();
			log.info("Received " + recordCount + " records");

			BulkRequest bulkRequest = new BulkRequest();

			for (ConsumerRecord<String, String> record : records) {

				try {
					// create id with Kafka
					// String id = record.topic() + record.partition() + record.offset();
					// get id from record, id is in metaObject
					String id = extractIdFromRecord(record.value());

					// send the record into OpenSearch
					IndexRequest  indexRequest = new IndexRequest("wikimedia")
							.source(record.value(), XContentType.JSON)
							.id(id);

					// assign index for every record
//							IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
					// assign index for bulk
					bulkRequest.add(indexRequest);

//							log.info(response.getId());
				} catch (Exception e) {
					// do nothing
				}

			}

			if (bulkRequest.numberOfActions() > 0) {
				BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
				log.info("Inserted into OpenSearch: " + bulkResponse.getItems().length + " records");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// commit offsets after batch is consumed
				consumer.commitSync();
				log.info("Offset have been committed");
			}

		}
	}

	private static void setShutdownHook(KafkaConsumer<String, String> consumer) {
		// get a reference to the main thread
		final Thread mainThread = Thread.currentThread();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
			consumer.wakeup();

			// join the main thread to allow the execution of the code in the main thread
			try {
				mainThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));
	}

	private static KafkaConsumer<String, String> createKafkaConsumer() {
		String bootstrapServers = "localhost:9092";
		String groupId = "consumer-opensearch-demo";

		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		return new KafkaConsumer<>(properties);
	}

	public static RestHighLevelClient createOpenSearchClient() {
		String connString = "http://localhost:9200";
//        String connString = "https://XXXXXXu@kXXXXX.bonsaisearch.net:443"; // to connect to Bonsai.io

		// we build a URI from the connection string
		RestHighLevelClient restHighLevelClient;
		URI connUri = URI.create(connString);
		// extract login information if it exists
		String userInfo = connUri.getUserInfo();

		if (userInfo == null) {
			// REST client without security
			restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

		} else {
			// REST client with security
			String[] auth = userInfo.split(":");

			CredentialsProvider cp = new BasicCredentialsProvider();
			cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

			restHighLevelClient = new RestHighLevelClient(
					RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
							.setHttpClientConfigCallback(
									httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
											.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
		}

		return restHighLevelClient;
	}

	private static String extractIdFromRecord(String json) {
		return JsonParser.parseString(json)
				.getAsJsonObject()
				.get("meta")
				.getAsJsonObject()
				.get("id")
				.getAsString();
	}

}
