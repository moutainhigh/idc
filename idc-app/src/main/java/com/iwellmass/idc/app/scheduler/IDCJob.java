package com.iwellmass.idc.app.scheduler;

import com.iwellmass.idc.JobEnv;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface IDCJob {

	@PostMapping(path = "/execution", consumes = MediaType.APPLICATION_JSON_VALUE)
	void execute(@RequestBody ExecuteRequest jobEnv);


	@PostMapping(path = "/execution", consumes = MediaType.APPLICATION_JSON_VALUE)
	void execute(@RequestBody JobEnv jobEnv);
}
