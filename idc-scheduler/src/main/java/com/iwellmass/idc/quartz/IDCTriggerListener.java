package com.iwellmass.idc.quartz;

import static com.iwellmass.idc.quartz.IDCContextKey.CONTEXT_INSTANCE;
import static com.iwellmass.idc.quartz.IDCContextKey.CONTEXT_INSTANCE_ID;
import static com.iwellmass.idc.quartz.IDCContextKey.CONTEXT_LOAD_DATE;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_DISPATCH_TYPE;
import static com.iwellmass.idc.quartz.IDCPlugin.toLocalDateTime;

import java.time.LocalDateTime;
import java.util.Date;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iwellmass.idc.model.DispatchType;
import com.iwellmass.idc.model.Job;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobInstanceStatus;
import com.iwellmass.idc.model.JobInstanceType;

public class IDCTriggerListener extends TriggerListenerSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDCTriggerListener.class);

	private final IDCPluginContext pluginContext;
	
	public IDCTriggerListener(IDCPluginContext pluginContext) {
		this.pluginContext = pluginContext;
	}

	@Override
	public void triggerFired(Trigger trigger, JobExecutionContext context) {
		
		LOGGER.info("触发任务 {}, loadDate {}", trigger.getJobKey(), context.getScheduledFireTime().getTime());
		
		DispatchType type = JOB_DISPATCH_TYPE.applyGet(context.getJobDetail().getJobDataMap());
		
		JobKey jobKey = trigger.getJobKey();

		boolean isRedo = false;

		JobInstance instance = null;
		
		if (context.isRecovering()) {
			// TODO
			throw new UnsupportedOperationException("not supported yet.");
		} else if (isRedo) {
			throw new UnsupportedOperationException("not supported yet.");
			/*int id = IDCConstants.CONTEXT_INSTANCE_ID.applyGet(context);
			JobInstance jobInstance = jobInstanceRepository.findOne(id);
			jobInstance.setStatus(JobInstanceStatus.NEW);
			jobInstance.setStartTime(LocalDateTime.now());
			jobInstance.setEndTime(null);
			instance = jobInstanceRepository.save(jobInstance);*/
		} else if (type == DispatchType.MANUAL) {
			
			LocalDateTime loadDate = CONTEXT_LOAD_DATE.applyGet(trigger.getJobDataMap());
			
			instance = pluginContext.createJobInstance(jobKey, (job) -> {
				JobInstance jobInstance = createJobInstance(job);
				jobInstance.setTriggerName(trigger.getKey().getName());
				jobInstance.setInstanceType(JobInstanceType.MANUAL);
				jobInstance.setLoadDate(loadDate);
				jobInstance.setNextLoadDate(null);
				jobInstance.setShouldFireTime(IDCPlugin.toMills(loadDate));
				return jobInstance;
			});
		} else if (type == DispatchType.AUTO) {
			
			Date shouldFireTime = context.getScheduledFireTime();
			Date nextFireTime = context.getNextFireTime();
			
			instance = pluginContext.createJobInstance(jobKey, (job) -> {
				JobInstance jobInstance = createJobInstance(job);
				jobInstance.setTriggerName(trigger.getKey().getName());
				jobInstance.setInstanceType(JobInstanceType.CRON);
				jobInstance.setLoadDate(toLocalDateTime(shouldFireTime));
				jobInstance.setNextLoadDate(toLocalDateTime(nextFireTime));
				jobInstance.setShouldFireTime(shouldFireTime == null ? -1 : shouldFireTime.getTime());
				return jobInstance;
			});
		}

		// 初始化执行环境
		CONTEXT_INSTANCE_ID.applyPut(context, instance.getInstanceId());
		CONTEXT_LOAD_DATE.applyPut(context, instance.getLoadDate());
		CONTEXT_INSTANCE.applyPut(context, instance);
		
	}

	private JobInstance createJobInstance(Job job) {
		JobInstance jobInstance = new JobInstance();
		jobInstance.setTaskId(job.getTaskId());
		jobInstance.setGroupId(job.getGroupId());
		jobInstance.setTaskName(job.getTaskName());
		jobInstance.setContentType(job.getContentType());
		jobInstance.setTaskType(job.getTaskType());
		jobInstance.setAssignee(job.getAssignee());
		jobInstance.setTaskType(job.getTaskType());
		jobInstance.setStatus(JobInstanceStatus.NEW);
		jobInstance.setParameter(job.getParameter());
		jobInstance.setStartTime(LocalDateTime.now());
		jobInstance.setEndTime(null);
		jobInstance.setScheduleType(job.getScheduleType());
		return jobInstance;
	}

	@Override
	public String getName() {
		return IDCTriggerListener.class.getName();
	}
}
