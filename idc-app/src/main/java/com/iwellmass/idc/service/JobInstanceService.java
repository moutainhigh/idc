package com.iwellmass.idc.service;

import static com.iwellmass.idc.quartz.IDCContextKey.CONTEXT_INSTANCE_ID;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_GROUP;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_ID;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_REOD;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_SCHEDULE_TYPE;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.iwellmass.common.exception.AppException;
import com.iwellmass.common.util.Assert;
import com.iwellmass.common.util.PageData;
import com.iwellmass.common.util.Pager;
import com.iwellmass.idc.app.model.CancleRequest;
import com.iwellmass.idc.app.model.RedoRequest;
import com.iwellmass.idc.executor.CompleteEvent;
import com.iwellmass.idc.model.ExecutionLog;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobInstanceStatus;
import com.iwellmass.idc.quartz.IDCContextKey;
import com.iwellmass.idc.quartz.IDCPlugin;
import com.iwellmass.idc.repo.ExecutionLogRepository;
import com.iwellmass.idc.repo.JobInstanceRepository;

@Service
public class JobInstanceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobInstanceService.class);
	
	@Inject
	private JobInstanceRepository jobInstanceRepository;
	
	@Inject
	private ExecutionLogRepository logRepository;

	@Inject
	private Scheduler scheduler;

	public void redo(RedoRequest request) {
		
		int instanceId = request.getInstanceId();
		JobInstance instance = jobInstanceRepository.findOne(instanceId);
		
		Assert.isTrue(instance != null, "找不到此实例");
		
		try {
			TriggerKey triggerKey = new TriggerKey("REDO_" + instanceId, instance.getJobGroup());
			
			Assert.isTrue(instance.getStatus().isComplete() || request.isForce(), "实例正在运行，无法重跑");
			
			Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(triggerKey)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.forJob(instance.getTaskId(), instance.getGroupId()) // 哪个业务
				.startNow().build();
			
			JobDataMap jdm = trigger.getJobDataMap();
			JOB_REOD.applyPut(jdm, true);
			JOB_ID.applyPut(jdm, instance.getJobId());
			JOB_GROUP.applyPut(jdm, instance.getJobGroup());
			JOB_SCHEDULE_TYPE.applyPut(jdm, instance.getScheduleType());
			CONTEXT_INSTANCE_ID.applyPut(jdm, instanceId);
			
			scheduler.scheduleJob(trigger);
		} catch (SchedulerException e) {
			throw new AppException("重跑失败: " + e.getMessage(), e);
		}
	}

	
	public void forceComplete(int instanceId) {
		
		JobInstance instance = jobInstanceRepository.findOne(instanceId);
		
		Assert.isTrue(instance != null, "找不到此实例");
		
		try {
			IDCPlugin plugin = IDCContextKey.IDC_PLUGIN.applyGet(scheduler.getContext());
			CompleteEvent event = CompleteEvent
				.successEvent("强制结束")
				.setInstanceId(instanceId)
				.setEndTime(LocalDateTime.now());
			plugin.fireCompleteEvent(event);
			scheduler.resetTriggerFromErrorState(IDCPlugin.toTriggerKey(instance.getJobPK()));
		} catch (SchedulerException e) {
			throw new AppException("强制结束任务时出错: " + e.getMessage());
		}
	}
	
	public void cancle(CancleRequest req) {
		int instanceId = req.getInstanceId();
		JobInstance instance = jobInstanceRepository.findOne(instanceId);
		Assert.isTrue(instance != null, "不存在此实例 %s", instanceId);
		
		try {
			IDCPlugin plugin = IDCContextKey.IDC_PLUGIN.applyGet(scheduler.getContext());
			plugin.cancleJob(instance.getJobId(), instance.getJobGroup());
			
		} catch (SchedulerException e) {
			LOGGER.error(e.getMessage(), e);
			logRepository.log(instanceId, "无法取消任务: {}", e.getMessage());
			if (req.isForce()) {
				instance.setStatus(JobInstanceStatus.CANCLED);
				logRepository.log(instanceId, "强制取消任务", e.getMessage());
			} else {
				throw new AppException("无法取消任务: " + e.getMessage(), e);
			}
		}
	}
	
	public PageData<ExecutionLog> getJobInstanceLog(Integer id, Pager pager) {
		Pageable page = new PageRequest(pager.getPage(), pager.getLimit(), new Sort(Direction.ASC, "id"));
		Page<ExecutionLog> data = logRepository.findByInstanceId(id, page);
		return new PageData<>((int) data.getTotalElements(), data.getContent());
	}

}
