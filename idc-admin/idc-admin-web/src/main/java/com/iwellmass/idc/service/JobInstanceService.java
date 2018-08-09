package com.iwellmass.idc.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.iwellmass.common.util.PageData;
import com.iwellmass.common.util.Pager;
import com.iwellmass.dispatcher.admin.DDCConfiguration;
import com.iwellmass.dispatcher.admin.dao.mapper.DdcTaskExecuteHistoryMapper;
import com.iwellmass.dispatcher.admin.dao.model.DdcTaskExecuteHistory;
import com.iwellmass.dispatcher.admin.service.ITaskService;
import com.iwellmass.dispatcher.common.constants.Constants;
import com.iwellmass.dispatcher.common.entry.DDCException;
import com.iwellmass.dispatcher.thrift.bvo.TaskTypeHelper;
import com.iwellmass.idc.mapper.IdcTaskHistoryMapper;
import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobQuery;

@Service
public class JobInstanceService {

    @Inject
    private IdcTaskHistoryMapper idcTaskHistoryMapper;

    @Inject
    private ITaskService iTaskService;
    
    @Inject
    private DdcTaskExecuteHistoryMapper histroyMapper;


    public PageData<JobInstance>findTaskInstanceByCondition(JobQuery query, Pager pager){
        Pager pager1=new Pager();
        pager1.setPage(pager.getTo());
        pager1.setLimit(pager.getLimit());
        if (query != null && query.getContentType() != null	) {
        	query.setContentType(TaskTypeHelper.classNameOf(query.getContentType()));
        }
        List<JobInstance> allTaskInstance = idcTaskHistoryMapper.findAllTaskInstanceByCondition(query);
        List<JobInstance> taskInstance = idcTaskHistoryMapper.findTaskInstanceByCondition(query, pager1);
        taskInstance.forEach(t -> {
        	t.setContentType(TaskTypeHelper.contentTypeOf(t.getContentType()));
        });
        return new PageData<JobInstance>(allTaskInstance.size(),taskInstance);
    }

    public List<JobQuery> getAllTypes(){
        List<JobQuery> list = new ArrayList<>();
        List<JobQuery> list1 = new ArrayList<>();
        idcTaskHistoryMapper.findAllTaskInstance().forEach(i -> {
           JobQuery query=new JobQuery();
           if(!(null==i.getContentType()||i.getContentType().equals(""))){
               query.setContentType(i.getContentType());
               list.add(query);
           }
        });
        for (JobQuery type : list) {
            boolean is = list1.stream().anyMatch(t -> t.getContentType().equals(type.getContentType()));
            if (!is) {
                list1.add(type);
            }
        }
        return list1;
    }

    public List<JobQuery> getAllAssignee(){
        List<JobQuery> list = new ArrayList<>();
        List<JobQuery> list1 = new ArrayList<>();
        idcTaskHistoryMapper.findAllTaskInstance().forEach(i -> {
            JobQuery query=new JobQuery();
            if(!(null==i.getAssignee()||i.getAssignee().equals(""))){
                query.setAssignee(i.getAssignee());
                list.add(query);
            }
        });
        for (JobQuery type : list) {
            boolean is = list1.stream().anyMatch(t -> t.getAssignee().equals(type.getAssignee()));
            if (!is) {
                list1.add(type);
            }
        }
        return list1;
    }

    public void redo(int id) throws DDCException {
    	
    	DdcTaskExecuteHistory history = histroyMapper.selectByPrimaryKey(Long.valueOf(id));
    	String batchId = history.getExecuteBatchId();
    	Long loadDate = history.getShouldFireTime();
    	Map<String, Object> parameters = new HashMap<>();
    	parameters.put("executeBatchId", batchId);
    	parameters.put("loadDate", loadDate);
    	
        iTaskService.executeTask(DDCConfiguration.DEFAULT_APP,id, Constants.TASK_TRIGGER_TYPE_MAN, parameters);
    }

}
