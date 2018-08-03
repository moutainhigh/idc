package com.iwellmass.idc.controller;

import com.iwellmass.common.ServiceResult;
import com.iwellmass.common.util.PageData;
import com.iwellmass.common.util.Pager;
import com.iwellmass.idc.model.Job;
import com.iwellmass.idc.model.JobQuery;
import com.iwellmass.idc.service.JobQueryService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;
@RestController
@RequestMapping("/job-query")
public class JobQueryController {

    @Inject
    private JobQueryService jobQueryService;

    @ApiOperation("通过条件检索任务（分页显示）")
    @PostMapping(path = "/findTasksByCondition")
    public ServiceResult<PageData<List<Job>>> findTasksByCondition(@RequestBody JobQuery query,
                                         @RequestParam(name = "page",defaultValue = "0") int page,
                                         @RequestParam(name = "pageSize",defaultValue = "10") int pageSize){
        Pager pager=new Pager();
        pager.setPage(page);
        pager.setLimit(pageSize);
        PageData<List<Job>> tasks = jobQueryService.findTasksByCondition(query, pager);
        return ServiceResult.success(tasks);
    }

    @ApiOperation("通过groupId查询上游任务")
    @GetMapping(path = "/findTaskByGroupId")
    public ServiceResult<List<Job>> findTaskByGroupId(Integer groupId){
        List<Job> taskByGroupId = jobQueryService.findTaskByGroupId(groupId);
        return ServiceResult.success(taskByGroupId);
    }

    @ApiOperation("获取所有实例类型")
    @GetMapping(path = "/getAllTypes")
    public ServiceResult<List<JobQuery>> getAllTypes(){
        List<JobQuery> allTypes =jobQueryService.getAllTypes();
        return ServiceResult.success(allTypes);
    }

    @ApiOperation("获取所有负责人")
    @GetMapping(path ="/getAllAssignee" )
    public ServiceResult<List<JobQuery>> getAllAssignee(){
        List<JobQuery> allAssignee = jobQueryService.getAllAssignee();
        return ServiceResult.success(allAssignee);
    }
}
