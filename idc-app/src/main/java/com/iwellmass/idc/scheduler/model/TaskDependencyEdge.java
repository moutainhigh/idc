package com.iwellmass.idc.scheduler.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "idc_task_dependency_edge")
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ApiModelProperty("依赖记录id")
    private Long id;

    @Column(name = "task_dependency_id")
    @ApiModelProperty("依赖计划图id")
    private Long taskDependencyId;

    @Column(name = "source")
    @ApiModelProperty("源计划")
    private String source;

    @Column(name = "target")
    @ApiModelProperty("被依赖计划名称")
    private String target;

    @Column(name = "principle")
    @ApiModelProperty("依赖规则")
    @Enumerated(value = EnumType.STRING)
    private Principle principle;

    @Column(name = "create_time")
    @ApiModelProperty("依赖创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    public TaskDependencyEdge(Long taskDependencyId,String source, String target) {
        this(taskDependencyId,source,target, getDefaultPrinciple());
    }

    // default: monthly dependency
    private static Principle getDefaultPrinciple() {
        return Principle.MONTHLY_2_MONTHLY;
    }

    public TaskDependencyEdge(Long taskDependencyId,String source, String target, Principle principle) {
        this.taskDependencyId = taskDependencyId;
        this.source = source;
        this.target = target;
        this.principle = principle;
        this.createTime = LocalDateTime.now();
    }
}
