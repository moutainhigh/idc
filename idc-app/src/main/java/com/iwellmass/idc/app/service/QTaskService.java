package com.iwellmass.idc.app.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.iwellmass.common.criteria.SpecificationBuilder;
import com.iwellmass.common.util.PageData;
import com.iwellmass.common.util.Pager;
import com.iwellmass.idc.TaskService;
import com.iwellmass.idc.app.mapper.TaskMapper;
import com.iwellmass.idc.app.repo.TaskRepository;
import com.iwellmass.idc.app.repo.WorkflowRepository;
import com.iwellmass.idc.app.vo.TaskQueryVO;
import com.iwellmass.idc.model.Task;
import com.iwellmass.idc.model.TaskKey;
import com.iwellmass.idc.model.TaskType;
import com.iwellmass.idc.model.Workflow;

@Service
public class QTaskService implements TaskService {

	@Inject
	TaskRepository taskRepository;

	@Inject
	TaskMapper taskMapper;
	
	@Inject
	private WorkflowRepository workflowRepo;
	
	@Override
	public Task getTask(TaskKey taskKey) {
		return taskRepository.findOne(taskKey);
	}

	@Override
	public List<Task> getTasks(List<TaskKey> taskKeys) {
		return taskMapper.selectBatch(taskKeys);
	}

	@Override
	public void saveTask(Task task) {
		
		if (task.getTaskId() == null) {
			task.setTaskId(System.currentTimeMillis() + "");
		}
		
		taskRepository.save(task);
	}

	@Override
	public List<Task> getTasksByType(TaskType taskType) {
		return taskRepository.findByTaskType(taskType);
	}

	public PageData<Task> queryTask(TaskQueryVO taskQuery, Pager pager) {
		
		PageRequest pageable = new PageRequest(pager.getPage(), pager.getLimit());
		
		Specification<Task> spec = taskQuery == null ? null : SpecificationBuilder.toSpecification(taskQuery);
		
		Page<Task> ret = taskRepository.findAll(spec, pageable);
		
		ret.getContent().forEach(task -> {
			
			Workflow wf = workflowRepo.findOne(task.getTaskKey());
			if (wf != null) {
				task.setGraphId(wf.getGraphId());
				task.setGraph(wf.getGraph());
			}
		});

		PageData<Task> task = new PageData<>((int)ret.getTotalElements(), ret.getContent());
		return task;
	}
}