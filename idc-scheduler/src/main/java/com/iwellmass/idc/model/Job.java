package com.iwellmass.idc.model;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iwellmass.common.param.ExecParam;
import com.iwellmass.idc.jpa.ExecParamConverter;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "t_idc_job")
@IdClass(JobKey.class)
public class Job {

	// ~~ 调度属性 ~~
	@Id
	@Column(name = "job_id", length = 50)
	private String jobId;

	@Id
	@Column(name = "job_group", length = 50)
	private String jobGroup;
	
	@Column(name = "job_name", length = 50)
	@ApiModelProperty("计划名称")
	private String jobName;
	
	@ApiModelProperty("责任人")
	@Column(name = "assignee")
	private String assignee;
	
	@ApiModelProperty("周期类型")
	@Column(name = "schedule_type")
	@Enumerated(EnumType.STRING)
	private ScheduleType scheduleType;
	
	@ApiModelProperty("出错重试")
	@Column(name = "is_retry")
	private Boolean isRetry;
	
	@ApiModelProperty("阻塞下游任务")
	@Column(name = "block_on_error")
	private Boolean blockOnError;
	
	@ApiModelProperty("创建日期")
	@Column(name = "start_time")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
	private LocalDateTime startTime;
	
	@ApiModelProperty("更新时间")
	@Column(name = "end_time")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
	private LocalDateTime endTime;
	
	@ApiModelProperty("创建日期")
	@Column(name = "createtime")
	private LocalDateTime createTime;

	@ApiModelProperty("更新时间")
	@Column(name = "update_time")
	private LocalDateTime updateTime;
	
	@ApiModelProperty("cron 表达式")
	@Column(name = "cron_expr")
	private String cronExpr;
	
	@ApiModelProperty("参数")
	@Column(name = "parameter", length = 4000)
	@Convert(converter = ExecParamConverter.class)
	private List<ExecParam> parameter;
	
	@Column(name = "schedule_config")
	private String scheduleConfig;
	
	// ~~ Task 相关 ~~
	@ApiModelProperty("业务ID")
	@Column(name = "task_id")
	private String taskId;

	@ApiModelProperty("业务域")
	@Column(name = "task_group")
	private String taskGroup;
	
	@ApiModelProperty("任务类型")
	@Column(name = "task_type")
	@Enumerated(EnumType.STRING)
	private TaskType taskType;

	@ApiModelProperty("业务类型")
	@Column(name = "content_type")
	private String contentType;
	
	@ApiModelProperty("执行方式")
	@Column(name = "dispatch_type")
	@Enumerated(EnumType.STRING)
	private DispatchType dispatchType;
	
	@ApiModelProperty("workflow_id")
	@Column(name = "workflow_id")
	private String workflowId;
	
	@Transient
	@JsonIgnore
	public JobKey getJobKey() {
		return new JobKey(jobId, jobGroup);
	}
	
	public void setJobKey(JobKey pk) {
		this.jobId = pk.getJobId();
		this.jobGroup = pk.getJobGroup();
	}
	
	@Transient
	@JsonIgnore
	public TaskKey getTaskKey() {
		return new TaskKey(taskId, taskGroup);
	}
	
	public void setTaskKey(TaskKey taskKey) {
		this.taskId = taskKey.getTaskId();
		this.taskGroup = taskKey.getTaskGroup();
	}

	@Override
	public String toString() {
		return "Job [jobId=" + jobId + ", jobGroup=" + jobGroup + ", jobName=" + jobName + "]";
	}
}
