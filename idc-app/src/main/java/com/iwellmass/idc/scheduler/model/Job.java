package com.iwellmass.idc.scheduler.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iwellmass.common.param.ExecParam;
import com.iwellmass.idc.scheduler.util.ExecParamConverter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "idc_job")
public class Job extends AbstractJob {

	static final Logger LOGGER = LoggerFactory.getLogger(Job.class);
	
	@Column(name = "task_name")
	private  String taskName;
	
	@Column(name = "task_group")
	private String taskGroup;
	
	@Column(name = "assignee", length = 20)
	private String assignee;
	
	@Column(name = "job_type")
	private JobType jobType;
	
	@Column(name = "load_date")
	private String loadDate;
	
	@Column(name = "param", columnDefinition = "TEXT")
	@Convert(converter = ExecParamConverter.class)
	private List<ExecParam> param;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumns({
		@JoinColumn(name = "task_name", referencedColumnName = "task_name", insertable = false, updatable = false),
		@JoinColumn(name = "task_group", referencedColumnName = "task_group", insertable = false, updatable = false)
	})
	private Task task;
	
	public Job() {
	}

	public Job(String id, Task task) {
		super(id, task);
		// 实例类型
		this.taskName = task.getTaskName();
		this.taskGroup = task.getTaskGroup();
		this.assignee = task.getAssignee();
		this.jobType = task.getScheduleType() == ScheduleType.MANUAL ? JobType.AUTO : JobType.MANUAL;
	}

	public void start() {
		if (task.getState().isRunning()) {
			throw new JobException("任务已关闭: " + task.getState());
		}
		super.start();
	}

	@Override
	void start0() {
		// 业务代码
	}
	
	
	
	public void renew() {
		if (state.isFailure()) {
			throw new JobException("Task already completed: " + this.state);
		}
	}

	public void finish() {
		this.state = JobState.FINISHED;
	}

	public void fail() {
		this.state = JobState.FAILED;
	}
}