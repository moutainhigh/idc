package com.iwellmass.idc.app.vo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.quartz.Trigger;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.iwellmass.common.param.ExecParam;
import com.iwellmass.idc.scheduler.model.ScheduleType;
import com.iwellmass.idc.scheduler.model.TaskType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@JsonTypeInfo(use = Id.NAME, property = "scheduleType")
@JsonSubTypes({
	@Type (name = "MANUAL", value = ManualTaskVO.class),
	@Type (name = "AUTO", value = CronTaskVO.class)
})
public abstract class TaskVO {
	
	@ApiModelProperty("计划名称")
	String taskName;

	@ApiModelProperty("描述")
	String description;

	@ApiModelProperty("负责人")
	String assignee;

	@ApiModelProperty("失败重试")
	Boolean isRetry = true;
	
	@ApiModelProperty("运行参数")
	List<ExecParam> parameter;
	
	@ApiModelProperty("调度方式")
	ScheduleType scheduleType;
	
	@ApiModelProperty("生效日期")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyyMMdd")
	LocalDate startDate;

	@ApiModelProperty("失效日期")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyyMMdd")
	LocalDate endDate;
	
	@ApiModelProperty("出错时阻塞")
	private Boolean blockOnError = true;
	
	// copy-of-task
	@ApiModelProperty("业务ID")
	String taskId;

	@ApiModelProperty("业务域")
	String taskGroup;

	@ApiModelProperty("任务类型")
	TaskType taskType;

	@ApiModelProperty("业务类型")
	String contentType;
	
	public abstract Trigger buildTrigger(String name, String group);
	
	@JsonIgnore
	public abstract Map<String, Object> getProps();
	
	public void setProps(Map<String, Object> props) {
		BeanUtils.copyProperties(props, this);
	}
	
}