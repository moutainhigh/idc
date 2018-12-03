package com.iwellmass.idc.app.repo;

import com.iwellmass.idc.model.WorkflowEdge;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowEdgeRepository extends CrudRepository<WorkflowEdge, Integer>,JpaSpecificationExecutor {

    Optional<WorkflowEdge> findByWorkflowId(Integer workflowId);
}