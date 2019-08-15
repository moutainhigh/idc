package com.iwellmass.idc.scheduler.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * 工作流节点，包装一个可执行的任务
 */
@Getter
@Setter
@Entity
@IdClass(WfID.class)
@Table(name = "idc_node_task")
public class NodeTask extends AbstractTask {
	
	public static final String START = "START";
	public static final String END = "END";
	public static final String CONTROL = "CONTROL";  // use to concurrent control

	/**
	 * 节点ID，本工作流内全局唯一
	 */
	@Id
	@Column(name = "id")
	private String id;

	@Column(name = "task_id")
	private String taskId;

	/**
	 * 业务类型
	 */
	@Column(name = "content_type")
	private String contentType;

}
