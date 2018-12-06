package com.iwellmass.idc.quartz;

import static com.iwellmass.idc.quartz.IDCContextKey.CONTEXT_INSTANCE;
import static com.iwellmass.idc.quartz.IDCContextKey.IDC_PLUGIN;
import static com.iwellmass.idc.quartz.IDCContextKey.JOB_RUNTIME;
import static com.iwellmass.idc.quartz.IDCContextKey.TASK_JSON;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.listeners.TriggerListenerSupport;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.iwellmass.common.exception.AppException;
import com.iwellmass.common.util.Assert;
import com.iwellmass.common.util.Utils;
import com.iwellmass.idc.DependencyService;
import com.iwellmass.idc.IDCLogger;
import com.iwellmass.idc.IDCPluginService;
import com.iwellmass.idc.IDCUtils;
import com.iwellmass.idc.SimpleIDCLogger;
import com.iwellmass.idc.executor.CompleteEvent;
import com.iwellmass.idc.executor.IDCStatusService;
import com.iwellmass.idc.executor.ProgressEvent;
import com.iwellmass.idc.executor.StartEvent;
import com.iwellmass.idc.model.DispatchType;
import com.iwellmass.idc.model.Job;
import com.iwellmass.idc.model.SubEnv;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobInstanceStatus;
import com.iwellmass.idc.model.JobKey;
import com.iwellmass.idc.model.PluginVersion;
import com.iwellmass.idc.model.ScheduleProperties;
import com.iwellmass.idc.model.ScheduleType;
import com.iwellmass.idc.model.Task;
import com.iwellmass.idc.model.TaskKey;
import com.iwellmass.idc.model.TaskType;

import lombok.Getter;
import lombok.Setter;

public abstract class IDCPlugin implements SchedulerPlugin, IDCConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDCPlugin.class);

	// ~~ init from factory ~~
	private Scheduler scheduler;
	private IDCJobStore idcJobStore;
	
	// ~~ internal component ~~
	@Getter
	private IDCStatusService statusService;
	
	@Getter
	private IDCPluginService pluginRepository;
	
	@Getter
	private DependencyService dependencyService;
	
	@Setter
	@Getter
	private IDCLogger logger = new SimpleIDCLogger();
	
	
	public IDCPlugin(IDCPluginService pluginRepository, DependencyService dependencyService) {
		this.pluginRepository = pluginRepository;
		this.dependencyService = dependencyService;
	}
	
	
	public void initialize(IDCJobStore store) {
		this.idcJobStore = store;
		store.clearAllBarrier();
	}
	
	@Override
	public void initialize(String name, Scheduler scheduler, ClassLoadHelper loadHelper) throws SchedulerException {
		LOGGER.info("加载 IDCPlugin...");
		
		Objects.requireNonNull(idcJobStore, "IDCJobStore cannot be null");
		Objects.requireNonNull(pluginRepository, "IDCSchedulerService cannot be null");
		Objects.requireNonNull(getDependencyService(), "DependencyService cannot be null");

		this.scheduler = scheduler;
		
		this. statusService = (IDCStatusService) Proxy.newProxyInstance(IDCPlugin.class.getClassLoader(), new Class[] {IDCStatusService.class}, new InvocationHandler() {
			private StdStatusService ss = new StdStatusService();
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					LOGGER.info("Get Event {}", args);
					return method.invoke(ss, args);
				} catch(Throwable e) {
					LOGGER.error("通知失败," + e.getMessage(), e);
				}
				return null;
			}
		});
		
		// set up context
		IDC_PLUGIN.applyPut(scheduler.getContext(), this);
		// add listeners
		scheduler.getListenerManager().addJobListener(new IDCJobListener());
		scheduler.getListenerManager().addTriggerListener(new IDCTriggerListener());
		scheduler.getListenerManager().addSchedulerListener(new IDCSchedulerListener());
		
		PluginVersion version = new PluginVersion();
		LOGGER.info("IDCPlugin 已加载, VERSION: {}", version.getVersion());
	}

	@Override
	public void start() {
		LOGGER.info("启动 IDCPlugin");
	}

	@Override
	public void shutdown() {
		LOGGER.info("停止 IDCPlugin");
	}
	
	/** 新增调度计划*/
	public void schedule(ScheduleProperties sp) throws SchedulerException {
		Task task = pluginRepository.findTask(new TaskKey(sp.getTaskId(), sp.getTaskGroup()));
		if (task == null) {
			throw new SchedulerException("任务不存在");
		}
		
		Job job = new Job();
		// 任务信息
		job.setJobKey(aquireJobKey(task));
		job.setJobName(task.getTaskName());
		job.setCreateTime(LocalDateTime.now());
		job.setUpdateTime(null);
		job.setTaskKey(task.getTaskKey());
		job.setTaskType(task.getTaskType());
		job.setContentType(task.getContentType());
		job.setDispatchType(sp.getDispatchType());
		// 调度信息
		job.setAssignee(sp.getAssignee());
		job.setScheduleType(sp.getScheduleType());
		job.setIsRetry(sp.getIsRetry());
		job.setBlockOnError(sp.getBlockOnError());
		if (sp.getStartTime() != null) {
			job.setStartTime(sp.getStartTime().atTime(LocalTime.MIN));
		}
		if (sp.getEndTime() != null) {
			job.setEndTime(sp.getEndTime().atTime(LocalTime.MAX));
		}
		job.setParameter(sp.getParameter());
		// ~~ 前端用 ~~
		job.setScheduleConfig(JSON.toJSONString(sp));
		if (job.getScheduleType() != ScheduleType.CUSTOMER) {
			job.setCronExpr(sp.toCronExpression());
		}
		pluginRepository.saveJob(job);
	
		
		// do scheduler
		if (job.getDispatchType() == DispatchType.AUTO) {
			scheduler.scheduleJob(buildJobDetail(task), buildAutoTrigger(job));
		} else {
			scheduler.scheduleJob(buildJobDetail(task), buildSimpleTrigger(job.getJobKey(), job.getTaskKey(), null));
		}
	}
	
	private JobKey aquireJobKey(Task tk) {
		return new JobKey(tk.getTaskId(), tk.getTaskGroup());
	}
	
	/**
	 * 重新调度
	 */
	public Date reschedule(JobKey jobKey, ScheduleProperties sp) throws SchedulerException {
		Job job = pluginRepository.findJob(jobKey);
		if (job == null) {
			throw new SchedulerException("调度计划 " + jobKey + " 不存在");
		}
		// 调度信息
		if (sp != null) {
			job.setAssignee(sp.getAssignee());
			job.setScheduleType(sp.getScheduleType());
			job.setIsRetry(sp.getIsRetry());
			job.setBlockOnError(sp.getBlockOnError());
			if (sp.getStartTime() != null) {
				job.setStartTime(sp.getStartTime().atTime(LocalTime.MIN));
			}
			if (sp.getEndTime() != null) {
				job.setEndTime(sp.getEndTime().atTime(LocalTime.MAX));
			}
			job.setParameter(sp.getParameter());
			job.setCronExpr(sp.toCronExpression());
			// ~~ 前端用 ~~
			job.setScheduleConfig(JSON.toJSONString(sp));
			pluginRepository.saveJob(job);
		}
		
		// do schedule
		Date ret = null;
		if (job.getDispatchType() == DispatchType.AUTO) {
			Trigger trigger = buildAutoTrigger(job);
			ret = scheduler.rescheduleJob(trigger.getKey(), trigger);
			if (ret == null) {
				Task task = pluginRepository.findTask(job.getTaskKey());
				scheduler.scheduleJob(buildJobDetail(task), trigger);
			}
		} else {
			Trigger trigger = buildSimpleTrigger(job.getJobKey(), job.getTaskKey(), null);
			ret = scheduler.rescheduleJob(trigger.getKey(), trigger);
			if (ret == null) {
				Task task = pluginRepository.findTask(job.getTaskKey());
				scheduler.scheduleJob(buildJobDetail(task), trigger);
			}
		}
		return ret;
	}
	List<TaskKey> scheduleSucessor(TaskKey srcTaskKey, Integer mainJobInsId) throws JobExecutionException {
		JobInstance mainJobIns;
		try {
			mainJobIns = idcJobStore.retrieveIDCJobInstance(mainJobInsId);
			return scheduleSucessor(srcTaskKey, mainJobIns);
		} catch (JobPersistenceException e) {
			throw new JobExecutionException(e.getMessage(), e);
		}
	}
	
	List<TaskKey> scheduleSucessor(TaskKey srcTaskKey, JobInstance mainJobIns) throws JobExecutionException {
		
		List<TaskKey> subTaskKeys = dependencyService.getSuccessors(mainJobIns.getWorkflowId(), srcTaskKey);
		if (subTaskKeys.isEmpty()) {
			return Collections.emptyList();
		}
		for (TaskKey subTask : subTaskKeys) {
			scheduleSubTask(subTask, mainJobIns);
		}
		return subTaskKeys;
	}
	
	private void scheduleSubTask(TaskKey subTaskKey, JobInstance mainJobIns) throws JobExecutionException {
		Task subTask = pluginRepository.findTask(subTaskKey);
		if (subTask == null) {
			throw new JobExecutionException("子任务不存在");
		}
		
		// 取出主任务
		Job mainJob = pluginRepository.findJob(mainJobIns.getJobKey());
		
		SubEnv subEnv = new SubEnv();
		// mark
		subEnv.setMainInstanceId(mainJobIns.getInstanceId());
		
		// build Task
		JobDetail jobDetail = buildJobDetail(subTask);
		
		// build Simple
		JobDataMap jobData = new JobDataMap();
		JOB_RUNTIME.applyPut(jobData, JSON.toJSONString(subEnv));

		//子任务Key
		JobKey subJobKey = aquireSubJobKey(mainJob.getJobKey(), subTask.getTaskKey());
		Trigger trigger = buildSimpleTrigger(subJobKey, subTaskKey, jobData);
		
		// just schedule
		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			throw new JobExecutionException(e.getMessage(), e);
		}
	}

	private JobKey aquireSubJobKey(JobKey mainJobKey, TaskKey subTaskKey) {
		String subJobId = mainJobKey.getJobId() + "_sub_" + subTaskKey.getTaskId();
		return new JobKey(subJobId, mainJobKey.getJobGroup());
	}
	
	private Trigger buildAutoTrigger(Job job) {
		// 构建 TriggerBuilder
		TriggerBuilder<CronTrigger> builder = TriggerBuilder.newTrigger()
			.withIdentity(IDCUtils.toTriggerKey(job.getJobKey()))
			.forJob(job.getTaskId(), job.getTaskGroup())
			.withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpr()).withMisfireHandlingInstructionIgnoreMisfires());
		// 设置开始时间
		Optional.ofNullable(job.getStartTime()).map(IDCUtils::toDate).ifPresent(builder::startAt);
		// 设置结束时间
		Optional.ofNullable(job.getEndTime()).map(IDCUtils::toDate).ifPresent(builder::endAt);
		
		return builder.build();
	}
	
	private Trigger buildSimpleTrigger(JobKey jobKey, TaskKey taskKey, JobDataMap jobData) {	// build Trigger
		// 构建常量
		TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
			.withIdentity(jobKey.getJobId(), jobKey.getJobGroup())
			.forJob(taskKey.getTaskId(), taskKey.getTaskGroup())
			.usingJobData(jobData)
			.withSchedule(SimpleScheduleBuilder.simpleSchedule());
		
		if (jobData != null) {
			builder.usingJobData(jobData);
		}
		return builder.build();
	}
	
	private JobDetail buildJobDetail(Task task) {
		JobDataMap jobData = new JobDataMap();
		TASK_JSON.applyPut(jobData, JSON.toJSONString(task));
		
		JobDetail jobDetail = null;
		if (task.getTaskType() == TaskType.WORKFLOW) {
			 jobDetail = JobBuilder.newJob(IDCWorkflowJob.class)
				 .withIdentity(task.getTaskId(), task.getTaskGroup())
				 .usingJobData(jobData)
				 .storeDurably()
				 .requestRecovery()
				 .build();
		} else {
			jobDetail = JobBuilder
				.newJob(getJobClass(task))
				.usingJobData(jobData)
				.withIdentity(task.getTaskId(), task.getTaskGroup())
				.requestRecovery()
				.build();
		}
		return jobDetail;
	}
	
	public void unschedule(JobKey jobKey) throws SchedulerException {
		Job job = pluginRepository.findJob(jobKey);
		
		Assert.isTrue(job != null, "调度计划 " + jobKey + "不存在");
		
		scheduler.unscheduleJob(IDCUtils.toTriggerKey(jobKey));
		
	}
	
	public void pause(JobKey jobKey) throws SchedulerException {
		scheduler.pauseTrigger(IDCUtils.toTriggerKey(jobKey));
	}
	
	public void resume(JobKey jobKey) throws SchedulerException {
		scheduler.resumeTrigger(IDCUtils.toTriggerKey(jobKey));
	}
	
	protected abstract Class<? extends org.quartz.Job> getJobClass(Task task);
	
	// ~~ 事件服务~~
	private class StdStatusService implements IDCStatusService {
		@Override
		public void fireStartEvent(StartEvent event) {
			logger.log(event.getInstanceId(), Optional.ofNullable(event.getMessage()).orElse("开始执行"));
			try {
				idcJobStore.storeIDCJobInstance(event.getInstanceId(), (jobInstance)->{
					jobInstance.setStartTime(event.getStartTime());
					jobInstance.setStatus(JobInstanceStatus.RUNNING);
				});
			} catch (Exception e) {
				String error = "更新任务状态出错" + e.getMessage();
				logger.log(event.getInstanceId(), error);
				throw new AppException(error, e);
			}
		}

		public void fireProgressEvent(ProgressEvent event) {
			logger.log(event.getInstanceId(), event.getMessage());
			try {
				idcJobStore.storeIDCJobInstance(event.getInstanceId(), (jobInstance)->{
					jobInstance.setStatus(JobInstanceStatus.RUNNING);
				});
			} catch (Exception e) {
				String error = "更新任务状态出错" + e.getMessage();
				logger.log(event.getInstanceId(), error);
				throw new AppException(error, e);
			}
		}
		
		@Override
		public void fireCompleteEvent(CompleteEvent event) {
			JobInstance ins = null;
			try {
				ins = idcJobStore.completeIDCJobInstance(event);
				if (ins == null) {
					return;
				}
			} catch (Exception e) {
				LOGGER.error("更新任务状态出错: " + e.getMessage());
			}

			// log this
			logger.log(event.getInstanceId(), event.getMessage()).log(event.getInstanceId(), "任务结束, 执行结果: {}", event.getFinalStatus());

			// 触发下游节点
			if (ins.getTaskType() == TaskType.SUB_TASK) {
				logger.log(ins.getMainInstanceId(), "子任务 {} 结束, 执行结果: {}", ins.getInstanceId(), event.getFinalStatus());
				try {
					List<TaskKey> list = scheduleSucessor(ins.getTaskKey(), ins);
					if (list.isEmpty()) {
						List<JobInstance> allSubs = idcJobStore.retrieveIDCSubJobInstance(ins.getMainInstanceId());

						if (allSubs.stream().filter(i -> i.getStatus() == JobInstanceStatus.FINISHED).count() > 0) {
							fireCompleteEvent(CompleteEvent.successEvent("子任务执行完毕").setEndTime(LocalDateTime.now()).setFinalStatus(JobInstanceStatus.FAILED)
									.setInstanceId(ins.getMainInstanceId()));
						} else {
							fireCompleteEvent(CompleteEvent.successEvent("子任务执行完毕").setEndTime(LocalDateTime.now()).setFinalStatus(JobInstanceStatus.FINISHED)
									.setInstanceId(ins.getMainInstanceId()));
						}
					}
				} catch (JobExecutionException e) {
					LOGGER.error(e.getMessage(), e);
				} catch (JobPersistenceException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}
	
	// ~~ Scheduler Listener ~~
	private class IDCSchedulerListener extends SchedulerListenerSupport {
	}

	// ~~ Trigger trace ~~
	private class IDCTriggerListener extends TriggerListenerSupport {
	
		@Override
		public void triggerFired(Trigger trigger, JobExecutionContext context) {
			try {
				JobInstance ins = idcJobStore.retrieveIDCJobInstance(Integer.parseInt(context.getFireInstanceId()));
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("触发任务 {}, TaskType: {}, DispatchType {}", ins.getJobKey(), ins.getTaskType(), ins.getDispatchType());
				}
				
				logger.clearLog(ins.getInstanceId())
				.log(ins.getInstanceId(), "创建任务实例 {}, 执行方式 {}, 任务类型 {}", ins.getInstanceId(), ins.getDispatchType(), ins.getTaskType())
				.log(ins.getInstanceId(), "周期类型 {}, 业务日期 {}, 批次 {}", ins.getScheduleType(), ins.getLoadDate(), IDCConstants.FULL_DF.format(new Date(ins.getShouldFireTime())))
				.log(ins.getInstanceId(), "运行参数: {}", Utils.isNullOrEmpty(ins.getParameter()) ? "--" : ins.getParameter());
				
				IDCContextKey.CONTEXT_INSTANCE.applyPut(context, ins);
			} catch (JobPersistenceException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		
		@Override
		public void triggerComplete(Trigger trigger, JobExecutionContext context, CompletedExecutionInstruction triggerInstructionCode) {
			super.triggerComplete(trigger, context, triggerInstructionCode);
		}
		
		@Override
		public String getName() {
			return IDCTriggerListener.class.getName();
		}
	}


	// ~~ Job trace ~~
	private class IDCJobListener extends JobListenerSupport {
		
		@Override
		public void jobToBeExecuted(JobExecutionContext context) {
			JobInstance instance = CONTEXT_INSTANCE.applyGet(context);
			
			// 记录任务
			if (instance.getTaskType() == TaskType.WORKFLOW) {
				statusService.fireProgressEvent(ProgressEvent.newEvent(instance.getInstanceId())
					.setStatus(JobInstanceStatus.NEW)
					.setMessage("执行工作流，业务日期 {}", instance.getLoadDate()));
			} else {
				statusService.fireProgressEvent(ProgressEvent.newEvent(instance.getInstanceId())
					.setStatus(JobInstanceStatus.NEW)
					.setMessage("派发任务，业务日期 {}", instance.getLoadDate()));
			}

			if (instance.getTaskType() == TaskType.SUB_TASK) {
				statusService.fireProgressEvent(ProgressEvent.newEvent(instance.getMainInstanceId())
						.setStatus(JobInstanceStatus.RUNNING)
						.setMessage("派发子任务 {}...", instance.getInstanceId()));
			}
			
		}
		
		@Override
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
			JobInstance instance = CONTEXT_INSTANCE.applyGet(context);
			if (jobException != null) {
				// 通知任务已经完成
				if (instance.getTaskType() == TaskType.WORKFLOW) {
					statusService.fireCompleteEvent(CompleteEvent.failureEvent()
						.setInstanceId(instance.getInstanceId())
						.setMessage("执行失败: {}", jobException.getMessage()));
				} else {
					statusService.fireCompleteEvent(CompleteEvent.failureEvent()
						.setInstanceId(instance.getInstanceId())
						.setMessage("派发任务失败: {}", jobException.getMessage()));
				}
			} else {
				// 本任务日志
				if (instance.getTaskType() != TaskType.WORKFLOW) {
					statusService.fireProgressEvent(ProgressEvent.newEvent(instance.getInstanceId())
							.setStatus(JobInstanceStatus.ACCEPTED)	
							.setMessage("等待执行结果...", instance.getInstanceId()));
				}
				if (instance.getTaskType() == TaskType.NODE_TASK) {
					statusService.fireProgressEvent(ProgressEvent.newEvent(instance.getMainInstanceId())
							.setStatus(JobInstanceStatus.ACCEPTED)	
							.setMessage("等待 {} 执行结果...", instance.getInstanceId()));
				}
			}
		}
		@Override
		public String getName() {
			return IDCJobListener.class.getSimpleName();
		}
	}
}
