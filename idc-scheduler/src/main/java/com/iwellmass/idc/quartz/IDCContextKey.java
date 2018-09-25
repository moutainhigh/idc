package com.iwellmass.idc.quartz;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerContext;

import com.iwellmass.idc.model.DispatchType;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.ScheduleType;

public class IDCContextKey<T> {

	// 缓存
	private static final Map<String, IDCContextKey<?>> cache = new HashMap<>();
	// NULL_VALUE
	private static final Object NIL = new Object();

	// ~~ 调度器 ~~
	public static final IDCContextKey<IDCPlugin> IDC_PLUGIN = defReq("idc.plugin", IDCPlugin.class);

	// ~~ JOB ~~
	public static final IDCContextKey<Boolean> JOB_ASYNC = defOpt("idc.job.async", Boolean.class, true);
	/** TaskId */
	public static final IDCContextKey<String> JOB_TASK_ID = defReq("idc.job.taskId", String.class);
	/** GroupId */
	public static final IDCContextKey<String> JOB_GROUP_ID = defReq("idc.job.groupId", String.class);
	/** ScheduleType */
	public static final IDCContextKey<ScheduleType> JOB_SCHEDULE_TYPE = defReq("idc.job.scheduleType", ScheduleType.class);
	/** 调度类型 */
	public static final IDCContextKey<DispatchType> JOB_DISPATCH_TYPE = defReq("idc.context.dispatchType", DispatchType.class);

	// ~~ runtime ~~
	/** 实例 ID */
	public static final IDCContextKey<Integer> CONTEXT_INSTANCE_ID = defReq("idc.context.instanceId", Integer.class);
	/** 获取业务日期 */
	public static final IDCContextKey<LocalDateTime> CONTEXT_LOAD_DATE = defReq("idc.context.loadDate", LocalDateTime.class);
	/** 实例对象 */
	public static final IDCContextKey<JobInstance> CONTEXT_INSTANCE = defReq("idc.context.jobInstance", JobInstance.class);
	/** 运行时参数 */
	public static final IDCContextKey<String> CONTEXT_PARAMETER = defReq("idc.context.parameter", String.class);
	
	

	private String key;
	private Class<T> type;
	private Object defaultValue = NIL;

	private IDCContextKey(String key, Class<T> valueType) {
		this.key = key;
		this.type = valueType;
	}

	public final String key() {
		return this.key;
	}

	public Class<T> valueType() {
		return this.type;
	}

	public T applyGet(SchedulerContext context) {
		Object o = context.get(this.key);
		return get0(o);
	}
	
	public T applyGet(JobDataMap jobDataMap) {
		Object o = jobDataMap.get(this.key);
		return get0(o);
	}

	@SuppressWarnings("unchecked")
	public final T applyGet(JobExecutionContext context) {
		Object o = get0(context.get(this.key()), false);
		if (o != NIL) {
			return (T) o;
		}
		o = get0(context.getTrigger().getJobDataMap().get(this.key), false);
		if (o != NIL) {
			return (T) o;
		}
		return (T) get0(context.getJobDetail().getJobDataMap().get(this.key), false);
	}
	
	public final void applyPut(SchedulerContext context, T v) {
		context.put(this.key, v);
	}
	
	public final void applyPut(JobDataMap map, T v) {
		map.put(this.key, v);
	}

	public final void applyPut(JobExecutionContext context, T v) {
		context.put(this.key, v);
	}
	
	@SuppressWarnings("unchecked")
	private T get0(Object o) {
		return (T) this.get0(o, true);
	}
	
	private Object get0(Object o, boolean required) {
		if (o == null) {
			if (defaultValue != NIL) {
				return  defaultValue;
			}
			if (required) {
				throw new NullPointerException("未设置 " + this.key + " 值");
			} else {
				return NIL;
			}
		}
		return  o;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IDCContextKey) {
			return key.equals(((IDCContextKey<?>) obj).key);
		}
		return key.equals(obj);
	}

	@Override
	public String toString() {
		return key;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	/** define required key-value */
	public static final <T> IDCContextKey<T> defReq(String key, Class<T> valueType) {
		IDCContextKey<T> contextKey = new IDCContextKey<>(key, valueType);
		cache.put(key, contextKey);
		return contextKey;
	}
	/** define option key-value */
	public static final <T> IDCContextKey<T> defOpt(String key, Class<T> valueType, T defaultValue) {
		IDCContextKey<T> contextKey = new IDCContextKey<>(key, valueType);
		contextKey.defaultValue = defaultValue;
		cache.put(key, contextKey);
		return contextKey;
	}

	public static final Collection<IDCContextKey<?>> keys() {
		return cache.values();
	}

}
