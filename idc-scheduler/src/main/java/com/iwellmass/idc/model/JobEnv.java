package com.iwellmass.idc.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobEnv {

	private Integer instanceId;
	
	private JobKey jobKey;

	private TaskKey taskKey;

	// 本次调度时间
	private Long shouldFireTime;
	
	// 上次调度时间
	private Long prevFireTime;
	
	// 责任人
	private String assignee;
	
	// 调度类型
	private ScheduleType scheduleType;
	
	// ~~ 执行参数 ~~
	private String parameter;
	
	// ~~ 节点类型 ~~
	private TaskType taskType;
	
	// ~~ SUB_TASK ~~
	private Integer mainInstanceId;
	
	private String workflowId;
	
}