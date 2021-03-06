package com.iwellmass.idc.app.controller;

import static com.iwellmass.idc.scheduler.util.IDCConstants.MSG_OP_SUCCESS;

import javax.inject.Inject;

import com.iwellmass.idc.app.vo.CloneWorkflowVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iwellmass.common.ServiceResult;
import com.iwellmass.common.util.PageData;
import com.iwellmass.idc.app.service.WorkflowService;
import com.iwellmass.idc.app.vo.WorkflowQueryParam;
import com.iwellmass.idc.app.vo.WorkflowVO;
import com.iwellmass.idc.app.vo.graph.GraphVO;

import io.swagger.annotations.ApiOperation;

import java.util.List;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

	@Inject
	private WorkflowService workflowService;

	@GetMapping
	@ApiOperation("查询工作流列表")
	public ServiceResult<PageData<WorkflowVO>> query(WorkflowQueryParam param) {
		PageData<WorkflowVO> data = workflowService.query(param);
		return ServiceResult.success(data);
	}

	@GetMapping("/all")
	@ApiOperation("查询所有工作流")
	public ServiceResult<List<WorkflowVO>> queryAll() {
		List<WorkflowVO> data = workflowService.queryAll();
		return ServiceResult.success(data);
	}

	@PostMapping
	@ApiOperation("新增工作流")
	public ServiceResult<String> save(@RequestBody WorkflowVO vo) {
		workflowService.save(vo);
		return ServiceResult.success(MSG_OP_SUCCESS);
	}

	@PostMapping(path = "/clone")
	@ApiOperation("克隆工作流")
	public ServiceResult<String> clone(@RequestBody CloneWorkflowVO vo) {
		workflowService.clone(vo);
		return ServiceResult.success(MSG_OP_SUCCESS);
	}

	@PutMapping
	@ApiOperation("更新工作流")
	public ServiceResult<String> update(@RequestBody WorkflowVO vo) {
		workflowService.update(vo);
		return ServiceResult.success(MSG_OP_SUCCESS);
	}
	

	@GetMapping("{id}")
	@ApiOperation("获取工作流")
	public ServiceResult<WorkflowVO> get(@PathVariable("id") String id) {
		WorkflowVO vo = workflowService.getWorkflow(id);
		return ServiceResult.success(vo);
	}
	
	@DeleteMapping("{id}")
	@ApiOperation("删除工作流")
	public ServiceResult<String> delete(@PathVariable("id") String id) {
		workflowService.delete(id);
		return ServiceResult.success(MSG_OP_SUCCESS);
	}
	
	@GetMapping("{workflowId}/graph")
	@ApiOperation("获取工作流依赖")
	public ServiceResult<GraphVO> getGraph(@PathVariable("workflowId") String workflowId) {
		GraphVO gvo = workflowService.getGraph(workflowId);
		return ServiceResult.success(gvo);
	}
	
	@PostMapping("{id}/graph")
	@ApiOperation("更新工作流依赖")
	public ServiceResult<String> updateGraph(@PathVariable("id") String id, @RequestBody GraphVO gvo) {
		workflowService.saveGraph(id, gvo);
		return ServiceResult.success(MSG_OP_SUCCESS);
	}

	@GetMapping("/available")
	@ApiOperation("查询可用的工作流")
	public ServiceResult<List<WorkflowVO>> queryAvailableWorkflow() {
		List<WorkflowVO> data = workflowService.queryAvailableWorkflow();
		return ServiceResult.success(data);
	}


}
