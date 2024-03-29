package com.pavelkostal.kafkaproducerwikimedia.tools;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
public class WikimediaChangeHandler implements EventHandler {

	KafkaProducer<String, String> kafkaProducer;
	String topic;

	public WikimediaChangeHandler(KafkaProducer<String, String> kafkaProducer, String topic) {
		this.kafkaProducer = kafkaProducer;
		this.topic = topic;
	}

	@Override
	public void onOpen() {
		// do nothing
	}

	@Override
	public void onClosed() {
		kafkaProducer.close();
	}

	@Override
	public void onMessage(String event, MessageEvent messageEvent) {
		log.info(messageEvent.getData());
		kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));
	}

	@Override
	public void onComment(String s) {
		// do nothing
	}

	@Override
	public void onError(Throwable throwable) {
		log.error("Error in stream reading", throwable);
	}
}
