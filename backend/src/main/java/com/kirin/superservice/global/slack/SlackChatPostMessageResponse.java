package com.kirin.superservice.global.slack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackChatPostMessageResponse(boolean ok, String error) {
}
