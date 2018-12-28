package com.iwellmass.idc.app.vo;

import com.iwellmass.common.criteria.In;
import com.iwellmass.common.criteria.Like;
import com.iwellmass.idc.model.TaskType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskQueryVO {

	@In
    private List<TaskType> taskType;

    @Like(builder = TempDefinedBuilder.class)
    private String taskName;
}
