package com.iwellmass.idc.client.autoconfig;

import static com.iwellmass.idc.executor.IDCJobExecutorService.toURI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import com.iwellmass.idc.JobEnv;
import com.iwellmass.idc.executor.IDCJob;
import com.iwellmass.idc.executor.IDCJobContext;

import javax.inject.Inject;

@Configuration
@ConditionalOnBean(IDCJob.class)
@EnableAsync
@EnableFeignClients(basePackages="com.iwellmass.idc.client.autoconfig")
@EnableEurekaClient
public class IDCClientAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(IDCClientAutoConfiguration.class);

	@Inject
	public RestIDCStatusService idcStatusManagerClient;


	@Bean(name = "idc-executor")
	public AsyncTaskExecutor asyncTaskExecutor() {
		return new SimpleAsyncTaskExecutor("idc-executor-");
	}


	/*
	 * 发现 IDCJob，将其注册为 rest 资源，url 为 /idc-job/{idc-job-content-contentType}/execution
	 * */
	@Bean
	public IDCJobHandlerMapping idcJobHandlerMapping(List<IDCJob> idcJobs, AutowireCapableBeanFactory autowire) {
		Map<String, IDCJobHandler> idcJobMap = new HashMap<>();
		for (IDCJob job : idcJobs) {
			IDCJobHandler jobHandler = new IDCJobHandler(job);
			autowire.autowireBean(jobHandler);
			LOGGER.info("注册 IDCJob '{}' -> {} ", job.getContentType(), job);
			String uri = toURI(job.getContentType());
			idcJobMap.put(uri, jobHandler);
		}
		IDCJobHandlerMapping mapping = new IDCJobHandlerMapping();
		mapping.setDynamicControllerMap(idcJobMap);
		mapping.setOrder(Ordered.HIGHEST_PRECEDENCE - 100);
		return mapping;
	}

}
