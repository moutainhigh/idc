package com.iwellmass.idc.quartz;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import com.iwellmass.idc.dag.WorkflowService;
import com.iwellmass.idc.executor.CompleteEvent;
import com.iwellmass.idc.model.DispatchType;
import com.iwellmass.idc.model.Job;
import com.iwellmass.idc.model.JobKey;
import com.iwellmass.idc.model.ScheduleProperties;
import com.iwellmass.idc.model.ScheduleType;
import com.iwellmass.idc.model.Task;
import com.iwellmass.idc.model.TaskKey;
import com.iwellmass.idc.model.TaskType;

public class IDCPluginTest {

	private static final String lqd_02 = "lqd_02";
	private static final String lqd_01 = "lqd_01";
	private static final String group = "test";

	private static final LocalDateTime _1_1 = LocalDateTime.of(LocalDate.of(2018, 1, 1), LocalTime.MIN);
	private static final LocalDateTime _9_1 = LocalDateTime.of(LocalDate.of(2018, 9, 1), LocalTime.MIN);

	@Test
	public void testSimpleSchedule() throws Exception {
		
		IDCPlugin plugin = new SimpleIDCPlugin();
		
		SimpleWorkflowService sws = new SimpleWorkflowService();
		
		IDCSchedulerFactory factory = new IDCSchedulerFactory();
		factory.setPlugin(plugin);
		factory.setWorkflowService(sws);
		factory.setDriverDelegate(new SimpleIDCDriverDelegate());
		factory.setDataSource(dataSource());
		

		Scheduler scheduler = factory.getScheduler();
		scheduler.clear();
		scheduler.start();
		
		ScheduleProperties sp = new ScheduleProperties();
		sp.setScheduleType(ScheduleType.MONTHLY);
		sp.setStartTime(_1_1);
		sp.setEndTime(_9_1);
		sp.setDays(Arrays.asList(-1));
		sp.setDuetime("00:00:00");

		// Job = JobDetails + Trigger
		Task task = new Task();
		task.setTaskKey(new TaskKey(lqd_01, group));
		task.setTaskName("simple task");
		task.setDispatchType(DispatchType.AUTO);
		task.setWorkflowId(1);
		task.setTaskType(TaskType.NODE_TASK);

		plugin.schedule(task, sp);

		scheduler.getTriggerState(new TriggerKey(lqd_01, group));
		
		Thread.sleep(500);
		plugin.getStatusService().fireCompleteEvent(CompleteEvent.successEvent().setInstanceId(123));
		
		Thread.sleep(10 * 60 * 1000);

	}
	@Test
	public void testWorkflowSchedule() throws Exception {
		
		IDCPlugin plugin = new SimpleIDCPlugin();
		
		SimpleWorkflowService sws = new SimpleWorkflowService();
		
		
		// 主任务
		Task task = new Task();
		task.setTaskKey(new TaskKey(lqd_02, group));
		task.setTaskName("工作流的任务");
		task.setDispatchType(DispatchType.AUTO);
		task.setTaskType(TaskType.WORKFLOW_TASK);
		task.setContentType("simple-test");
		task.setWorkflowId(1);
		
		// 子任务
		Task sub1 = new Task();
		sub1.setTaskKey(new TaskKey(lqd_02 + "_sub1", group));
		sub1.setTaskName("sub1");
		sub1.setDispatchType(DispatchType.AUTO);
		sub1.setTaskType(TaskType.WORKFLOW_SUB_TASK);
		sub1.setContentType("simple-test");
		sub1.setWorkflowId(1);
		
		sws.addTask(1, sub1, WorkflowService.START, WorkflowService.END);
		
		IDCSchedulerFactory factory = new IDCSchedulerFactory();
		factory.setPlugin(plugin);
		factory.setWorkflowService(sws);
		factory.setDriverDelegate(new SimpleIDCDriverDelegate());
		factory.setDataSource(dataSource());
		
		Scheduler scheduler = factory.getScheduler();
		scheduler.clear();
		scheduler.start();
		
		
		ScheduleProperties sp = new ScheduleProperties();
		sp.setScheduleType(ScheduleType.MONTHLY);
		sp.setStartTime(_1_1);
		sp.setEndTime(_9_1);
		sp.setDays(Arrays.asList(-1));
		sp.setDuetime("00:00:00");
		
		plugin.schedule(task, sp);
		
		scheduler.getTriggerState(new TriggerKey(lqd_02, group));
		
		Thread.sleep(10 * 60 * 1000);
		
	}

	private DataSource dataSource() {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
		dataSource.setUrl("jdbc:mysql://austin.realhyx.com:3306/test?characterEncoding=utf8&useSSL=false");
		dataSource.setUsername("root");
		dataSource.setPassword("000000oO)");
		return dataSource;
	}
}
