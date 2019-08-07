package com.iwellmass.idc.app.service;

import com.iwellmass.idc.app.message.TaskEventPlugin;
import com.iwellmass.idc.app.scheduler.ExecuteRequest;
import com.iwellmass.idc.app.scheduler.JobEnvAdapter;
import com.iwellmass.idc.message.FinishMessage;
import com.iwellmass.idc.scheduler.IDCJobExecutors;
import com.iwellmass.idc.scheduler.model.*;
import com.iwellmass.idc.scheduler.quartz.IDCJobStore;
import com.iwellmass.idc.scheduler.quartz.ReleaseInstruction;
import com.iwellmass.idc.scheduler.repository.AllJobRepository;
import com.iwellmass.idc.scheduler.repository.JobRepository;
import com.iwellmass.idc.scheduler.repository.WorkflowRepository;
import com.iwellmass.idc.scheduler.service.IDCLogger;
import lombok.Setter;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

@Component
public class JobHelper {

    @Setter
    private Scheduler scheduler;

    @Autowired
    private IDCLogger idcLogger;
    @Autowired
    IDCJobStore idcJobStore;
    @Autowired
    AllJobRepository allJobRepository;
    @Autowired
    WorkflowRepository workflowRepository;

    // 启动
    public void start(AbstractJob job) {
        if (job.getState().isComplete()) {
            throw new JobException("任务已执行");
        }
        if (job.getTask() == null) {
            throw new JobException("任务不存在");
        }
        if (job.getTaskType() == TaskType.WORKFLOW) {
            executeJob((Job) job);
        } else {
            executeNodeJob((NodeJob) job);
        }
    }

    // 重跑
    public void renew(AbstractJob job) {
        checkRunning(job);
        job.setUpdatetime(LocalDateTime.now());
    }

    // 成功
    public void success(AbstractJob job) {
        checkRunning(job);
        job.setState(JobState.FINISHED);
        onJobFinished(job);
    }

    // 失败
    public void failed(AbstractJob job) {
        checkRunning(job);
        job.setState(JobState.FAILED);
    }

    // 重跑
    public void redo(AbstractJob job) {
        // TODO 编写重做逻辑
    }

    // 取消
    public void cancle(AbstractJob job) {
        // TODO 编写取消逻辑
    }

    // 跳过
    public void skip(AbstractJob job){}

    private void checkRunning(AbstractJob job) {
        if (job.getState().isComplete()) {
            throw new JobException("任务已结束: " + job.getState());
        }
    }

    public void startJob(AbstractJob job) {
        if (job.getTaskType() == TaskType.WORKFLOW) {
            executeJob((Job) job);
        } else {
            executeNodeJob((NodeJob) job);
        }
    }

    public void executeJob(Job job) {
        idcLogger.log(job.getId(), "执行workflow id={}", job.getTask().getWorkflow().getId());
        job.setState(JobState.RUNNING);
        runNextJob(job, NodeTask.START);
    }

    public void executeNodeJob(NodeJob job) {
        NodeTask task = (NodeTask) Objects.requireNonNull(job.getTask(), "未找到任务");
        if (job.getNodeId().equals(NodeTask.END)) {
            idcLogger.log(job.getId(), "执行task end,container={}", job.getContainer());
            job.setState(JobState.FINISHED);
            FinishMessage message = FinishMessage.newMessage(job.getContainer());
            message.setMessage("执行结束");
            TaskEventPlugin.eventService(scheduler).send(message);
            return;
        }
        job.setState(JobState.RUNNING);
        idcLogger.log(job.getId(), "执行task id={}, task = {},container={}", job.getId(), job.getTask().getTaskId(), job.getContainer());
        ExecuteRequest request = new ExecuteRequest();
        request.setDomain(task.getDomain());
        request.setContentType(task.getType());
        JobEnvAdapter jobEnvAdapter = new JobEnvAdapter();
        jobEnvAdapter.setTaskId(task.getTaskId());
        jobEnvAdapter.setInstanceId(job.getId());
        request.setJobEnvAdapter(jobEnvAdapter);
        IDCJobExecutors.getExecutor().execute(request);
    }


    public void runNextJob(Job job, String startNode) {
        AbstractTask task = Objects.requireNonNull(job.getTask(), "未找到任务");
        Workflow workflow = Objects.requireNonNull(task.getWorkflow(), "未找到工作流");
        // 找到立即节点
        Set<String> successors = workflow.successors(startNode);
        Iterator<NodeJob> iterator = job.getSubJobs().stream()
                .filter(sub -> successors.contains(sub.getNodeId()))
                .iterator();

        while (iterator.hasNext()) {
            NodeJob next = iterator.next();
            try {
                Set<String> previous = workflow.getPrevious(next.getNodeId());

                //如果存在未完成的任务 则不继续执行
                boolean unfinishJob = job.getSubJobs().stream()
                        .filter(sub -> previous.contains(sub.getNodeId()))
                        .anyMatch(sub -> !sub.getState().isSuccess());
                if (!unfinishJob) {
                    startJob(next);
                }
            } catch (Exception e) {
                e.printStackTrace();
                next.setState(JobState.FAILED);
                job.setState(JobState.FAILED);
            }
        }
    }

    // job finish
    public void onJobFinished(AbstractJob runningJob) {
        if (runningJob instanceof Job) {
            // Release trigger
            Job job = (Job) runningJob;
            TriggerKey tk = job.getTask().getTriggerKey();
            if (job.getState().isComplete()) {
                if (job.getState().isSuccess()) {
                    idcJobStore.releaseTrigger(tk, ReleaseInstruction.RELEASE);
                } else {
                    idcJobStore.releaseTrigger(tk, ReleaseInstruction.SET_ERROR);
                }
            }
        } else {
            NodeJob nodeJob = (NodeJob) runningJob;
            Workflow workflow = workflowRepository.findById(nodeJob.getWorkflowId()).get();
            Job parent = (Job) allJobRepository.findById(nodeJob.getContainer()).get();
            parent.getTask().setWorkflow(workflow);
            runNextJob(parent, nodeJob.getNodeId());
        }
    }
}
