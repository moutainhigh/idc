package com.iwellmass.idc.scheduler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.iwellmass.idc.scheduler.model.Task;
import com.iwellmass.idc.scheduler.model.TaskID;

@Repository
public interface TaskRepository extends CrudRepository<Task, TaskID>, JpaSpecificationExecutor<Task>{
	
//	@Query("SELECT t FROM Task t WHERE name = ?1 AND group = '" + Task.GROUP + "'")
	default Optional<Task> findByName(String name){
		return Optional.empty();
	};

	@Query("SELECT DISTINCT assignee FROM Task WHERE assignee IS NOT NULL")
	List<String> findAllAssignee();
}
