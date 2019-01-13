package com.iwellmass.idc.app.controller;

import java.util.List;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.*;

import com.iwellmass.common.ServiceResult;
import com.iwellmass.common.util.PageData;
import com.iwellmass.common.util.Pager;
import com.iwellmass.idc.app.service.JobInstanceQueryService;
import com.iwellmass.idc.app.service.JobInstanceService;
import com.iwellmass.idc.app.vo.Assignee;
import com.iwellmass.idc.app.vo.CancleRequest;
import com.iwellmass.idc.app.vo.JobInstanceQuery;
import com.iwellmass.idc.app.vo.RedoRequest;
import com.iwellmass.idc.model.ExecutionLog;
import com.iwellmass.idc.model.JobInstance;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/job-instance")
public class JobInstanceController {

    @Inject
    private JobInstanceService jobInstanceService;

    @Inject
    private JobInstanceQueryService queryService;

    @ApiOperation("通过条件检索实例（分页显示）")
    @PostMapping("/query")
    public ServiceResult<PageData<JobInstance>> queryJobInstance(
            @RequestBody(required = false) JobInstanceQuery query, Pager pager) {
        PageData<JobInstance> taskInstance = queryService.findJobInstance(query, pager);
        return ServiceResult.success(taskInstance);
    }

    @ApiOperation("获取所有责任人")
    @GetMapping("/assignee")
    public ServiceResult<List<Assignee>> assignee() {
        return ServiceResult.success(queryService.getAllAssignee());
    }

    @ApiOperation("获取子任务实例")
    @GetMapping("/{id}/sub-job-instance")
    public ServiceResult<List<JobInstance>> getWorkflowTask(@PathVariable("id") Integer id) {
        List<JobInstance> result = queryService.getWorkflowSubInstance(id);
        return ServiceResult.success(result);
    }

    @ApiOperation("重跑任务")
    @PostMapping("/{id}/redo")
    public ServiceResult<String> restart(@PathVariable(name = "id") Integer id, @RequestBody(required = false) RedoRequest redoRequest) {

        if (redoRequest == null) {
            redoRequest = new RedoRequest();
        }
        redoRequest.setInstanceId(id);
        jobInstanceService.redo(redoRequest);
        return ServiceResult.success("success");
    }

    @ApiOperation("取消任务")
    @PostMapping("/{id}/cancle")
    public ServiceResult<String> cancle(@PathVariable(name = "id") Integer id, @RequestBody(required = false) CancleRequest redoRequest) {
        redoRequest.setInstanceId(id);
        jobInstanceService.cancle(redoRequest);
        return ServiceResult.success("success");
    }

    @ApiOperation("强制结束任务")
    @PostMapping("/{id}/force-complete")
    public ServiceResult<String> forceComplete(@PathVariable(name = "id") Integer id) {
        jobInstanceService.forceComplete(id);
        return ServiceResult.success("success");
    }

    @ApiOperation("任务日志(分页)")
    @PostMapping("/{id}/log")
    public ServiceResult<PageData<ExecutionLog>> getLog(@PathVariable(name = "id") Integer id, Pager pager) {
        PageData<ExecutionLog> data = jobInstanceService.getJobInstanceLog(id, pager);
        return ServiceResult.success(data);
    }

}
