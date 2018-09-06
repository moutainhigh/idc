package com.iwellmass.idc.quartz;

import static com.iwellmass.idc.quartz.IDCPlugin.toLocalDateTime;

import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.listeners.SchedulerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.iwellmass.idc.model.Job;
import com.iwellmass.idc.model.JobInstanceType;
import com.iwellmass.idc.model.ScheduleStatus;
import com.iwellmass.idc.model.Sentinel;
import com.iwellmass.idc.model.SentinelStatus;
import com.iwellmass.idc.repo.JobRepository;
import com.iwellmass.idc.repo.SentinelRepository;

@Component
public class IDCQuartzSchedulerListener extends SchedulerListenerSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDCQuartzSchedulerListener.class);

	@Inject
	private JobRepository jobRepository;
	
	@Inject
	private SentinelRepository sentinelRepository;
	
	@Override
	public void jobScheduled(Trigger trigger) {
		
		JobKey jobKey = trigger.getJobKey();
		
		Job job = jobRepository.findOne(jobKey.getName(), jobKey.getGroup());
		job.setStatus(ScheduleStatus.NORMAL);
		
		Date nextFireTime = Optional.ofNullable(trigger.getNextFireTime()).get();
		if (nextFireTime != null) {
			JobInstanceType type = IDCConstants.CONTEXT_JOB_INSTANCE_TYPE.applyGet(trigger.getJobDataMap());
			TriggerKey triggerKey = IDCPlugin.buildTriggerKey(type, jobKey.getName(), jobKey.getGroup());
			Sentinel sentinel = new Sentinel();
			sentinel.setTriggerName(triggerKey.getName());
			sentinel.setTriggerGroup(triggerKey.getGroup());
			sentinel.setShouldFireTime(nextFireTime.getTime());
			sentinel.setStatus(SentinelStatus.READY);
			sentinelRepository.save(sentinel);
			LOGGER.info("create '{}.{}.{}' sentinel", jobKey.getName(), jobKey.getGroup(), IDCPlugin.DEFAULT_LOAD_DATE_DF.format(nextFireTime));
			
			job.setPrevLoadDate(null);
			job.setNextLoadDate(toLocalDateTime(nextFireTime));
		}
	}
	
	@Override
	public void jobUnscheduled(TriggerKey triggerKey) {
		JobKey jobKey = IDCPlugin.toJobKey(triggerKey);
		Job job = getJob(jobKey);
		job.setStatus(ScheduleStatus.COMPLETE);
		jobRepository.save(job);
	}
	
	@Override
	public void schedulerError(String msg, SchedulerException cause) {
		LOGGER.info("调度错误: " + msg, cause);
	}
	
	@Override
	public void triggerResumed(TriggerKey triggerKey) {
		JobKey key = IDCPlugin.toJobKey(triggerKey);
		Job job = getJob(key);
		job.setStatus(ScheduleStatus.NORMAL);
		jobRepository.save(job);
	}

	@Override
	public void triggerPaused(TriggerKey triggerKey) {
		JobKey key = IDCPlugin.toJobKey(triggerKey);
		Job job = getJob(key);
		job.setStatus(ScheduleStatus.PAUSED);
		jobRepository.save(job);
	}
	
	@Override
	public void triggerFinalized(Trigger trigger) {
		Job job = getJob(trigger.getJobKey());
		job.setStatus(ScheduleStatus.COMPLETE);
		jobRepository.save(job);
	}
	
	private Job getJob(JobKey jobKey) {
		String taskId = jobKey.getName();
		String groupId = jobKey.getGroup();
		return jobRepository.findOne(taskId, groupId);
	};
}