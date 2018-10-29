package com.iwellmass.idc.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iwellmass.idc.model.JobInstance;
import com.iwellmass.idc.model.JobKey;

@Repository
public interface JobInstanceRepository
		extends PagingAndSortingRepository<JobInstance, Integer>, JpaSpecificationExecutor<JobInstance> {

	@Query("SELECT u FROM JobInstance u WHERE u.instanceId = ?1")
	JobInstance findOne(Integer id);

	@Query("SELECT u FROM JobInstance u WHERE u.jobId = ?1 and u.jobGroup = ?2 and loadDate = ?3")
	JobInstance findOne(String jobId, String jobGroup, LocalDateTime loadDate);

	@Query("SELECT DISTINCT assignee FROM JobInstance WHERE assignee IS NOT NULL")
	List<String> findAllAssignee();

	@Modifying
	@Query("DELETE FROM JobInstance WHERE jobId = :#{#jk.jobId} AND jobGroup = :#{#jk.jobGroup}")
	void deleteByJob(@Param("jk") JobKey jobPk);
}
