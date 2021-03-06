package com.iwellmass.idc.app.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iwellmass.common.param.ExecParam;
import com.iwellmass.datafactory.common.vo.TaskDetailVO;
import com.iwellmass.idc.app.rpc.DFTaskService;
import com.iwellmass.idc.app.vo.TaskDependencyQueryVO;
import com.iwellmass.idc.app.vo.graph.*;
import com.iwellmass.idc.app.vo.task.*;
import com.iwellmass.idc.model.CronType;
import com.iwellmass.idc.model.ScheduleType;
import com.iwellmass.idc.scheduler.model.*;
import com.iwellmass.idc.scheduler.repository.*;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.iwellmass.common.criteria.SpecificationBuilder;
import com.iwellmass.common.exception.AppException;
import com.iwellmass.common.util.PageData;
import com.iwellmass.idc.app.vo.Assignee;
import com.iwellmass.idc.app.vo.TaskQueryParam;
import com.iwellmass.idc.app.vo.TaskRuntimeVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class TaskService {

    // the latest day of last month compared to shouldFireTime
    private static final String LAST_DAY_OF_LAST_MONTH_COMPARED_SHOULDFIRETIME = "调度批次上月的最后一天";
    private static final String LAST_DAY_OF_LAST_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.plusMonths(-1).with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";
    private static final String LAST_DAY_OF_THIS_MONTH_COMPARED_SHOULDFIRETIME = "调度批次当月的最后一天";
    private static final String LAST_DAY_OF_THIS_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";
    private static final String LAST_DAY_OF_NEXT_MONTH_COMPARED_SHOULDFIRETIME = "调度批次下月的最后一天";
    private static final String LAST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.plusMonths(1).with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";

    // the first day of month compared to shouldFireTime
    private static final String FIRST_DAY_OF_LAST_MONTH_COMPARED_SHOULDFIRETIME = "调度批次上月的第一天";
    private static final String FIRST_DAY_OF_LAST_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.plusMonths(-1).with(@TemporalAdjusters@firstDayOfMonth()).format('yyyyMMdd')";
    private static final String FIRST_DAY_OF_THIS_MONTH_COMPARED_SHOULDFIRETIME = "调度批次当月的第一天";
    private static final String FIRST_DAY_OF_THIS_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.with(@TemporalAdjusters@firstDayOfMonth()).format('yyyyMMdd')";
    private static final String FIRST_DAY_OF_NEXT_MONTH_COMPARED_SHOULDFIRETIME = "调度批次下月的第一天";
    private static final String FIRST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_SHOULDFIRETIME = "#idc.shouldFireTime.plusMonths(1).with(@TemporalAdjusters@firstDayOfMonth()).format('yyyyMMdd')";

    // the latest day of last month compared to realRunTime
    private static final String LAST_DAY_OF_LAST_MONTH_COMPARED_REALRUNTIME = "实际运行日期上月的最后一天";
    private static final String LAST_DAY_OF_LAST_MONTH_OGNL_COMPARED_REALRUNTIME = "#idc.realRunTime.plusMonths(-1).with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";
    private static final String LAST_DAY_OF_THIS_MONTH_COMPARED_REALRUNTIME = "实际运行日期当月的最后一天";
    private static final String LAST_DAY_OF_THIS_MONTH_OGNL_COMPARED_REALRUNTIME = "#idc.realRunTime.with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";
    private static final String LAST_DAY_OF_NEXT_MONTH_COMPARED_REALRUNTIME = "实际运行日期下月的最后一天";
    private static final String LAST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_REALRUNTIME = "#idc.realRunTime.plusMonths(1).with(@TemporalAdjusters@lastDayOfMonth()).format('yyyyMMdd')";
    // now
    private static final String NOW = "调度计划提交日期";
    private static final String NOW_OGNL = "#idc.taskUpdateTime.format('yyyyMMdd')";

    static final Map<String, String> loadDateParams = new LinkedHashMap<>();

    static {
        loadDateParams.put(LAST_DAY_OF_LAST_MONTH_COMPARED_SHOULDFIRETIME, LAST_DAY_OF_LAST_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(LAST_DAY_OF_THIS_MONTH_COMPARED_SHOULDFIRETIME, LAST_DAY_OF_THIS_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(LAST_DAY_OF_NEXT_MONTH_COMPARED_SHOULDFIRETIME, LAST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(FIRST_DAY_OF_LAST_MONTH_COMPARED_SHOULDFIRETIME, FIRST_DAY_OF_LAST_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(FIRST_DAY_OF_THIS_MONTH_COMPARED_SHOULDFIRETIME, FIRST_DAY_OF_THIS_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(FIRST_DAY_OF_NEXT_MONTH_COMPARED_SHOULDFIRETIME, FIRST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_SHOULDFIRETIME);
        loadDateParams.put(LAST_DAY_OF_LAST_MONTH_COMPARED_REALRUNTIME, LAST_DAY_OF_LAST_MONTH_OGNL_COMPARED_REALRUNTIME);
        loadDateParams.put(LAST_DAY_OF_THIS_MONTH_COMPARED_REALRUNTIME, LAST_DAY_OF_THIS_MONTH_OGNL_COMPARED_REALRUNTIME);
        loadDateParams.put(LAST_DAY_OF_NEXT_MONTH_COMPARED_REALRUNTIME, LAST_DAY_OF_NEXT_MONTH_OGNL_COMPARED_REALRUNTIME);
        loadDateParams.put(NOW, NOW_OGNL);
    }

    @Resource
    TaskRepository taskRepository;
    @Resource
    JobRepository jobRepository;
    @Resource
    WorkflowRepository workflowRepository;
    @Resource
    NodeJobRepository nodeJobRepository;
    @Resource
    TaskDependencyEdgeRepository taskDependencyEdgeRepository;
    @Resource
    TaskDependencyRepository taskDependencyRepository;
    @Inject
    JobService jobService;

    @Setter
    private Scheduler scheduler;

    @Inject
    DFTaskService dfTaskService;

    public TaskVO getTask(String name) {

        Task task = taskRepository.findById(new TaskID(name)).orElseThrow(() -> new AppException("任务不存在"));
        TaskVO vo;
        if (task.getScheduleType() == ScheduleType.AUTO) {
            vo = new CronTaskVO();
            BeanUtils.copyProperties(task, vo, "workflow");
            ((CronTaskVO) vo).setCronType(CronType.valueOf(task.getProps().get("cronType").toString()));
            if (((CronTaskVO) vo).getCronType().equals(CronType.MONTHLY)) {
                ((CronTaskVO) vo).setDays((List<Integer>) task.getProps().get("days"));
            } else if (((CronTaskVO) vo).getCronType().equals(CronType.CUSTOMER)) {
                ((CronTaskVO) vo).setExpression(String.valueOf(task.getProps().getOrDefault("expression", "未配置")));
            }
        } else {
            vo = new ManualTaskVO();
            BeanUtils.copyProperties(task, vo, "workflow");
        }
        if (task.getStartDateTime() != null) {
            vo.setStartDate(task.getStartDateTime().toLocalDate());
        }
        if (task.getEndDateTime() != null) {
            vo.setEndDate(task.getEndDateTime().toLocalDate());
        }
        List<MergeTaskParamVO> mergeTaskParamVOS = getParams(task.getWorkflowId());
        for (MergeTaskParamVO m : mergeTaskParamVOS) {
            for (ExecParam p : task.getParams()) {
                if (m.getExecParam().getName().equals(p.getName())) {
                    m.getExecParam().setDefaultExpr(p.getDefaultExpr());
                    break;
                }
            }
        }
        vo.setMergeTaskParamVOS(mergeTaskParamVOS);
        return vo;
    }

    public PageData<TaskRuntimeVO> query(TaskQueryParam jqm) {
        Specification<Task> spec = SpecificationBuilder.toSpecification(jqm);
        Page<TaskRuntimeVO> taskRuntimeVOPage = taskRepository
                .findAll(spec, PageRequest.of(jqm.getPage(), jqm.getLimit(), Sort.by(Sort.Direction.DESC, "createtime")))
                .map(t -> {
                    TaskRuntimeVO vo = new TaskRuntimeVO();
                    BeanUtils.copyProperties(t, vo);
                    vo.setWorkflowName(t.getWorkflow().getWorkflowName());
                    vo.setCanDelete(canDelete(t.getTaskName()));
                    twiceValidateState(t, vo);
                    return vo;
                });
        return new PageData<>((int) taskRuntimeVOPage.getTotalElements(), taskRuntimeVOPage.getContent());
    }

    public List<Assignee> getAllAssignee() {
        return taskRepository.findAllAssignee().stream().map(Assignee::new).collect(Collectors.toList());
    }

    // twice validate task'state when complete ,the task is like to be running or error
    public void twiceValidateState(Task task, TaskRuntimeVO taskRuntimeVO) {
        if (task.getState().equals(TaskState.COMPLETE)) {
            List<Job> jobs = jobRepository.findAllByTaskName(task.getTaskName());
            if (jobs.size() == 0) {
                taskRuntimeVO.setState(TaskState.NONE);
                return;
            }

            Collections.sort(jobs, (o1, o2) -> {
                String sub01 = o1.getId().substring(o1.getId().lastIndexOf("-"));
                String sub02 = o2.getId().substring(o2.getId().lastIndexOf("-"));
                return (int) (Long.valueOf(sub01) - Long.valueOf(sub02));
            });

            for (Job job : jobs) {
                if (job.getState().equals(JobState.NONE) || job.getState().equals(JobState.RUNNING)) {
                    taskRuntimeVO.setState(TaskState.EXECUTING);
                    return;
                }
                if (job.getState().equals(JobState.FAILED)) {
                    taskRuntimeVO.setState(TaskState.ERROR);
                    return;
                }

                // compatible with manual task state.because in manual task,a task can contain success job and fail job.
                // in manual task,the task'state stand for the state of the last of this task
                if (job.getState().equals(JobState.FINISHED)) {
                    taskRuntimeVO.setState(TaskState.COMPLETE);
                    return;
                }
            }
        }
    }

    public List<MergeTaskParamVO> getParams(String workflowId) {
        List<NodeTask> nodeTasks = workflowRepository.findById(workflowId).orElseThrow(() -> new AppException("未发现指定工作流:" + workflowId)).getNodeTasks();
        List<TaskDetailVO> taskDetailVOS = dfTaskService.batchQueryTaskInfo(nodeTasks.stream()
                .filter(nt -> !nt.getTaskId().equalsIgnoreCase("start") &&
                        !nt.getTaskId().equalsIgnoreCase("end") &&
                        !nt.getTaskId().equalsIgnoreCase("control")
                )
                .map(nt -> Long.valueOf(nt.getTaskId()))
                .collect(Collectors.toList()))
                .getResult();
        return taskDetailVOS == null ? Lists.newArrayList() : buildMergeTaskParamVOS(taskDetailVOS.stream().map(TaskParamVO::new).collect(Collectors.toList()));
    }

    public List<MergeTaskParamVO> buildMergeTaskParamVOS(List<TaskParamVO> taskParamVOS) {
        Map<String, MergeTaskParamVO> mergeTaskParamVOS = Maps.newHashMap();
        taskParamVOS.forEach(t -> t.getParams().forEach(p -> {
            if (mergeTaskParamVOS.keySet().stream().noneMatch(m -> m.equals(p.getName()))) {
                mergeTaskParamVOS.put(p.getName(), new MergeTaskParamVO(t.getNodeTaskName(), p));
            } else {
                mergeTaskParamVOS.get(p.getName()).setMergedNodeTaskName(String.join("、", mergeTaskParamVOS.get(p.getName()).getMergedNodeTaskName(), t.getNodeTaskName()));
            }
        }));
        return mergeTaskParamVOS.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public List<String> getLoadDateParams() {
        return Lists.newArrayList(loadDateParams.keySet());
    }

    @Transactional
    public void delete(String taskName) {
        if (!canDelete(taskName)) {
            throw new AppException("删除失败：该调度计划存在未结束的实例任务");
        }
        // delete nodeJob job task
        List<Job> jobs = jobRepository.findAllByTaskName(taskName);
        List<NodeJob> nodeJobs = nodeJobRepository.findAllByContainerIn(jobs.stream().map(Job::getId).collect(Collectors.toList()));
        // nodeJob
        nodeJobRepository.deleteAll(nodeJobs);
        // job
        jobRepository.deleteAll(jobs);
        // task
        Task task = taskRepository.findById(new TaskID(taskName)).orElseThrow(() -> new AppException("未发现taskName[%s]计划", taskName));
        // task dependency
//        taskDependencyRepository.deleteBySourceOrTarget(taskName, taskName);

        try {
            scheduler.unscheduleJob(task.getTriggerKey());
            taskRepository.delete(task);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * judge the task whether can be deleted
     *
     * @param taskName
     * @return
     */
    private boolean canDelete(String taskName) {
        return nodeJobRepository.findAllByContainerIn(
                jobRepository.findAllByTaskName(taskName).stream().map(Job::getId).collect(Collectors.toList())
        ).stream().filter(nj -> !nj.isSystemNode()).allMatch(n -> n.getState().isComplete());
    }

    public TaskGraphVO getTaskDependenciesByTaskName(String taskName) {
        return getTaskDependenciesById(taskDependencyEdgeRepository.findAllBySourceOrTarget(taskName,taskName)
                .stream()
                .map(TaskDependencyEdge::getTaskDependencyId)
                .distinct()
                .findFirst()
                .orElseThrow(() -> new AppException("该调度计划[%s]不存在于任何计划依赖中",taskName)));
    }

    public TaskGraphVO getTaskDependenciesById(Long taskDependencyId) {
        TaskDependency taskDependency = taskDependencyRepository.findById(taskDependencyId).orElseThrow(() -> new AppException("未发现指定任务依赖[%s]",taskDependencyId));
        TaskGraphVO taskGraphVO = new TaskGraphVO();
        taskDependency.getEdges().forEach(edge -> {
            addTaskNodeVOToTaskGraphVO(new TaskNodeVO(jobService.getLatestJobByTaskName(edge.getTarget()), edge.getTarget()), taskGraphVO);
            addTaskNodeVOToTaskGraphVO(new TaskNodeVO(jobService.getLatestJobByTaskName(edge.getSource()), edge.getSource()), taskGraphVO);
        });
        return taskGraphVO;
    }

    // do not use.but this method can work.adopt getTaskDependenciesById instead.
    private TaskGraphVO findRecordByTaskNameAsSourceAndTarget(String taskName, TaskGraphVO taskGraphVO, Set<String> taskNamesProcessed) {
        if (!taskNamesProcessed.contains(taskName)) {
            taskNamesProcessed.add(taskName);
            addTaskNodeVOToTaskGraphVO(new TaskNodeVO(jobService.getLatestJobByTaskName(taskName), taskName), taskGraphVO);
            List<TaskDependencyEdge> targetTaskDependencies = taskDependencyEdgeRepository.findAllBySource(taskName);
            List<TaskDependencyEdge> sourceTaskDependencies = taskDependencyEdgeRepository.findAllByTarget(taskName);
            targetTaskDependencies.forEach(td -> {
                addTaskNodeVOToTaskGraphVO(new TaskNodeVO(jobService.getLatestJobByTaskName(td.getTarget()), td.getTarget()), taskGraphVO);
                addEdgeVOToTaskGraphVO(new EdgeVO(td.getId().toString(), new SourceVO(taskName), new TargetVO(td.getTarget())), taskGraphVO);
                findRecordByTaskNameAsSourceAndTarget(td.getTarget(), taskGraphVO, taskNamesProcessed);
            });
            sourceTaskDependencies.forEach(td -> {
                addTaskNodeVOToTaskGraphVO(new TaskNodeVO(jobService.getLatestJobByTaskName(td.getSource()), td.getSource()), taskGraphVO);
                addEdgeVOToTaskGraphVO(new EdgeVO(td.getId().toString(), new SourceVO(td.getSource()), new TargetVO(taskName)), taskGraphVO);
                findRecordByTaskNameAsSourceAndTarget(td.getSource(), taskGraphVO, taskNamesProcessed);
            });
        }
        return taskGraphVO;
    }

    private void addTaskNodeVOToTaskGraphVO(TaskNodeVO taskNodeVO, TaskGraphVO taskGraphVO) {
        if (taskGraphVO.getNodes().stream().map(TaskNodeVO::getId).noneMatch(id -> taskNodeVO.getId().equals(id))) {
            taskGraphVO.getNodes().add(taskNodeVO);
        }
    }

    private void addEdgeVOToTaskGraphVO(EdgeVO edgeVO, TaskGraphVO taskGraphVO) {
        if (taskGraphVO.getEdges().stream().map(EdgeVO::getId).noneMatch(id -> edgeVO.getId().equals(id))) {
            taskGraphVO.getEdges().add(edgeVO);
        }
    }

    public List<TaskRuntimeVO> getTaskToDependency(CronType cronType) {
        return Lists.newArrayList(taskRepository.findAll()).stream()
                .filter(tk -> tk.getProps() != null && CronType.valueOf(tk.getProps().get("cronType").toString()) == cronType)
                .map(tk -> {
                    TaskRuntimeVO vo = new TaskRuntimeVO();
                    BeanUtils.copyProperties(tk, vo);
                    vo.setWorkflowName(tk.getWorkflow().getWorkflowName());
                    vo.setCanDelete(canDelete(tk.getTaskName()));
                    twiceValidateState(tk, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    public void saveDependency(TaskDependencyVO taskDependencyVO) {
        taskDependencyRepository.findByName(taskDependencyVO.getName()).ifPresent(td -> {
            throw new AppException("当前计划依赖图名称已存在");
        });
        taskDependencyRepository.save(new TaskDependency(taskDependencyVO));
    }

    @Transactional
    public void saveDependencyEdge(Long id, TaskGraphVO taskGraphVO) {
        Optional<TaskDependency> taskDependency = taskDependencyRepository.findById(id);
        if (!taskDependency.isPresent()) {
            throw new AppException("当前计划依赖图名称不存在");
        }

        if (taskGraphVO.getEdges().isEmpty() || taskGraphVO.getNodes().isEmpty()) {
            throw new AppException("未配置任何边或者节点");
        }

        // clear old
        taskDependencyEdgeRepository.deleteByTaskDependencyId(id);

        // validate graph
        DirectedAcyclicGraph<String, EdgeVO> taskDependencyGraph = new DirectedAcyclicGraph<>(EdgeVO.class); // vertex:id, edge:EdgeVO
        Map<String, TaskNodeVO> nodeMap = taskGraphVO.getNodes().stream()  // <id,TaskNodeVO>
                .collect(Collectors.toMap(TaskNodeVO::getId, Function.identity()));
        nodeMap.keySet().forEach(taskDependencyGraph::addVertex);

        for (EdgeVO evo : taskGraphVO.getEdges()) {
            taskDependencyGraph.addEdge(evo.getSource().getId(), evo.getTarget().getId(), evo);
        }

        taskDependencyGraph.vertexSet().forEach(v -> {
            if (taskDependencyGraph.inDegreeOf(v) == 0 && taskDependencyGraph.outDegreeOf(v) == 0) {
                throw new AppException("节点" + v + "不能独立");
            }
        });

        // save new graph
        List<TaskDependencyEdge> edges = taskGraphVO.getEdges().stream()
                .map(edge -> new TaskDependencyEdge(id, edge.getSource().getId(), edge.getTarget().getId(), taskDependency.get().getPrinciple()))
                .collect(Collectors.toList());
        taskDependencyEdgeRepository.saveAll(edges);

        // todo jobHelper.notifyJobInWaitSet();
    }

    /**
     * @param taskDependencyId
     * @return all taskNames contained by this taskDependency
     */
    public Set<String> getAllTaskInTaskDependency(Long taskDependencyId) {
        TaskDependency taskDependency = taskDependencyRepository.findById(taskDependencyId).orElseThrow(() -> new AppException("未发现该任务依赖图taskDependencyId[%s]", taskDependencyId));
        Set<String> sourceTaskNames = taskDependency.getEdges().stream().map(TaskDependencyEdge::getSource).collect(Collectors.toSet());
        Set<String> targetTaskNames = taskDependency.getEdges().stream().map(TaskDependencyEdge::getTarget).collect(Collectors.toSet());
        return Stream.of(sourceTaskNames, targetTaskNames).collect(Collector.of(
                HashSet::new,
                Set::addAll,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                }));
    }

    /**
     * query dependency by taskName or taskDependencyName.
     * @return
     */
    public List<TaskDependency> queryTaskDependencies(TaskDependencyQueryVO taskDependencyQueryVO) {
        return taskDependencyRepository.findAll(null, Sort.by(Sort.Direction.DESC, "updatetime"))
                .stream()
                .filter(td -> taskDependencyQueryVO == null || taskDependencyQueryVO.getName() == null || taskDependencyQueryVO.getName().equals(td.getName()))
                .filter(td -> taskDependencyQueryVO == null || taskDependencyQueryVO.getTaskName() == null || getAllTaskInTaskDependency(td.getId()).contains(taskDependencyQueryVO.getTaskName()))
                .collect(Collectors.toList());
    }

    public List<Task> queryCanDrawTask(Long taskDependencyId) {
        Set<String> taskNames = getAllTaskInTaskDependency(taskDependencyId);
        return taskRepository.findAll(null,Sort.by(Sort.Direction.DESC, "createtime"))
                .stream()
                .filter(tk -> tk.getScheduleType() == ScheduleType.AUTO &&
                        tk.getProps() != null &&
                        tk.getProps().getOrDefault("cronType","").equals(CronType.MONTHLY.name()) &&
                        !taskNames.contains(tk.getTaskName()))
                .collect(Collectors.toList());
    }

}