package com.hjj.lingxibi.bizmq;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MQMessage implements Serializable {

    private Long chartId;

    private Long teamId;

    private Long invokeUserId;

}
