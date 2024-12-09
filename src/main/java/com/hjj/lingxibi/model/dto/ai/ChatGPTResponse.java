package com.hjj.lingxibi.model.dto.ai;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class ChatGPTResponse {

    private String content;

    private int totalTokens;
}
