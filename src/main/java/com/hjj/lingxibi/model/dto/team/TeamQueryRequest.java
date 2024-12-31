package com.hjj.lingxibi.model.dto.team;

import com.hjj.lingxibi.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQueryRequest extends PageRequest implements Serializable {

    private String searchParams;

}
