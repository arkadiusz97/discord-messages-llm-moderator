package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultClientTest {

    @Test
    public void shouldSendPrompt() {
        var builder = mock(Builder.class, RETURNS_SELF);
        var chatClient = mock(ChatClient.class);
        var requestSpec = mock(ChatClientRequestSpec.class);
        var callResponseSpec = mock(CallResponseSpec.class);
        var inputMessage = "input message";

        when(builder.build()).thenReturn(chatClient);

        when(chatClient.prompt(inputMessage)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        var expected = new PromptResponse(false, "output message");
        when(callResponseSpec.entity(PromptResponse.class)).thenReturn(expected);

        var defaultClient = new DefaultClient(builder, "command");

        PromptResponse result = defaultClient.sendPrompt(new PromptRequest(inputMessage));

        assertThat(result).isEqualTo(expected);
        verify(chatClient, times(1)).prompt(inputMessage);
        verify(callResponseSpec, times(1)).entity(PromptResponse.class);
    }

}
