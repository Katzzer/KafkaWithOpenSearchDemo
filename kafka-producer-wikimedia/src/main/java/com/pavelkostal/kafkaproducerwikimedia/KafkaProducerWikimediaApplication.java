package com.pavelkostal.kafkaproducerwikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.pavelkostal.kafkaproducerwikimedia.tools.WikimediaChangeHandler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class KafkaProducerWikimediaApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaProducerWikimediaApplication.class, args);
	}

	@Bean
	public CommandLineRunner myCommandLineRunner() {
		return args -> {
			String bootstrapServers = "localhost:9092";

			Properties properties = new Properties();
			properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
			properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        	properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

			KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

			String topic = "wikimedia.recentchange";

			EventHandler eventHandler = new WikimediaChangeHandler(producer, topic);

			String url = "https://stream.wikimedia.org/v2/stream/recentchange";
			EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
			EventSource eventSource = builder.build();

			eventSource.start();

			TimeUnit.MINUTES.sleep(10); // run this app for 10 minutes

		};
	}

}
