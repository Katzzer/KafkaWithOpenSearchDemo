package com.pavelkostal.kafkaconsumeropensearch;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;

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
			try (openSearchClient){
				boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);

				if (indexExists) {
					CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
					openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT); // creates "wikimedia" index in OpenSearch
					log.info("Index wikimedia create");
				} else {
					log.info("Index wikimedia already exists");
				}

			}



		};
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
}
