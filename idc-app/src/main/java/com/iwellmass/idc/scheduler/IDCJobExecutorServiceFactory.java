package com.iwellmass.idc.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.iwellmass.idc.executor.IDCJobExecutorService;
import com.iwellmass.idc.model.JobInstance;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;

@Component
@Import(FeignClientsConfiguration.class)
public class IDCJobExecutorServiceFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(IDCJobExecutorServiceFactory.class);

	@Inject
	private Decoder decoder;

	@Inject
	private Encoder encoder;

	@Inject
	private Client client;

	@Inject
	private Contract contract;

	private Map<String, IDCJobExecutorService> registryMap = new ConcurrentHashMap<>();

	public IDCJobExecutorService getExecutor(JobInstance instance) {
		IDCJobExecutorService service = registryMap.computeIfAbsent(instance.getGroupId(), (key) -> {
			return newFeignClient(instance.getGroupId(), instance.getContentType());
		});
		return service;
	}

	private IDCJobExecutorService newFeignClient(String domain, String contentType) {
		String path = "http://" + domain + IDCJobExecutorService.toURI(contentType);
		LOGGER.info("create fegin client: {}", path);
		RestIDCJobExecutor feginClient = Feign.builder().client(client).encoder(encoder).decoder(decoder)
				.contract(contract).target(RestIDCJobExecutor.class, path);
		return feginClient;
	}

}