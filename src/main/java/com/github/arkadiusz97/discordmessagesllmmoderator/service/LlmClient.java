package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;

public interface LlmClient {

    PromptResponse sendPrompt(PromptRequest promptRequest);

}
