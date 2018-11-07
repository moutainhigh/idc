package com.iwellmass.idc.quartz;

import static com.iwellmass.idc.quartz.IDCContextKey.IDC_PLUGIN;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iwellmass.common.util.Utils;
import com.iwellmass.idc.IDCUtils;
import com.iwellmass.idc.TwoTuple;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobKey;

/**
 * 同步生成 JobInstance 记录
 */
public class IDCTriggerListener extends TriggerListenerSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDCTriggerListener.class);

	@Override
	public void triggerFired(Trigger trigger, JobExecutionContext context) {
		
		IDCPlugin idcPlugin = IDC_PLUGIN.applyGet(context.getScheduler());
		
		TwoTuple<JobKey, Long> jobInsKey = IDCUtils.parseJobInstanceKey(trigger);
		
		JobInstance ins = idcPlugin.getJobInstance(jobInsKey._1, jobInsKey._2);
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("任务 {} 已触发, DispatchType {}", ins.getJobKey(), ins.getInstanceType());
		}
		
		idcPlugin.getLogger().clearLog(ins.getInstanceId())
			.log(ins.getInstanceId(), "创建任务实例 {}, 实例类型 {} ", ins.getInstanceId(), ins.getInstanceType())
			.log(ins.getInstanceId(), "运行参数: {}", Utils.isNullOrEmpty(ins.getParameter()) ? "--" : ins.getParameter());
		
		IDCContextKey.CONTEXT_INSTANCE.applyPut(context, ins);
		
	}
	
	@Override
	public String getName() {
		return IDCTriggerListener.class.getName();
	}
}
