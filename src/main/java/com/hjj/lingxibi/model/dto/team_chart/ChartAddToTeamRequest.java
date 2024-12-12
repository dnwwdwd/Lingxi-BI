package com.hjj.lingxibi.model.dto.team_chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChartAddToTeamRequest implements Serializable {

    private Long chartId;

    private Long teamId;

}
