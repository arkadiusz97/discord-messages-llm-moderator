package com.github.arkadiusz97.discordmessagesllmmoderator.model;

public record PromptResponse(
        Boolean breaksRules,
        String reasonForBreakingRules
) { }
