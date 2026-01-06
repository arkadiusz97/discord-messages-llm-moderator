package com.github.arkadiusz97.discordmessagesllmmoderator.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class Config {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter, @Value("${app.queue-prefetch-count}") Integer queuePrefetchCount) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(queuePrefetchCount);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(converter);
        return factory;
    }

    @Bean
    public Queue myQueue(@Value("${app.queue-name}") String queueName) {
        return new Queue(queueName);
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient(@Value("${app.discord-bot-token}") String discordBotToken) {
        return DiscordClientBuilder.create(discordBotToken)
                .build()
                .login()
                .block();
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor(
            @Value("${app.thread-pool-size-for-messages-handler}") int threadPoolSizeForMessagesHandler
    ) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setVirtualThreads(true);
        exec.setThreadNamePrefix("discord-messages-handler-");
        exec.setCorePoolSize(threadPoolSizeForMessagesHandler);
        return exec;
    }

    @Bean
    public RestClient.Builder restClientBuilder(
            HttpClient httpClient,
            @Value("${app.rest-client-timeout-seconds}") long restClientTimeoutSeconds
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(restClientTimeoutSeconds));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    public HttpClient httpClient(@Value("${app.rest-client-timeout-seconds}") long restClientTimeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(restClientTimeoutSeconds))
                .build();
    }

}
