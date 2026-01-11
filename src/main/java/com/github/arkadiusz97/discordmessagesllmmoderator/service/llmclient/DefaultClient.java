package com.github.arkadiusz97.discordmessagesllmmoderator.service.llmclient;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultClient implements LlmClient {

    private final ChatClient chatClient;

    public DefaultClient(ChatClient.Builder chatClientBuilder, @Value("${app.system-command}") String systemCommand) {
        this.chatClient = chatClientBuilder
                .defaultSystem(systemCommand)
                .build();
    }

    public PromptResponse sendPrompt(PromptRequest promptRequest) {
        return chatClient
                .prompt(promptRequest.message())
                .call()
                .entity(PromptResponse.class);
    }

}
