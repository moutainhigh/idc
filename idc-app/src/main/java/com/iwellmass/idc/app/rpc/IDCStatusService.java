package com.iwellmass.idc.app.rpc;

import com.iwellmass.idc.app.message.TaskEventPlugin;
import com.iwellmass.idc.executor.CompleteEvent;
import com.iwellmass.idc.executor.ProgressEvent;
import com.iwellmass.idc.executor.StartEvent;
import com.iwellmass.idc.message.FinishMessage;
import com.iwellmass.idc.message.*;
import com.iwellmass.idc.model.JobInstanceStatus;
import com.iwellmass.idc.scheduler.quartz.IDCJobStore;
import io.swagger.annotations.ApiOperation;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;

@RestController
@RequestMapping("/job")
public class IDCStatusService {

	@Inject
	private TaskEventPlugin taskEventPlugin;
	@Resource
	Scheduler qs;

	@Inject
	private IDCJobStore idcJobStore;

	final Logger logger = LoggerFactory.getLogger(getClass());
	@ApiOperation("任务开始")
	@PutMapping("/start")
	public void fireStartEvent(@RequestBody StartEvent event) {
		logger.info("fireStartEvent",event.getNodeJobId());
//		taskEventPlugin.send(StartMessage.newMessage(String.valueOf(event.getNodeJobId())));

//		StartMessage message = StartMessage.newMessage(jobId);
//		message.setMessage("启动任务");
//		TaskEventPlugin.eventService(context.getScheduler()).send(message);
//		idcPlugin.getStatusService().fireStartEvent(event);
//		idcJobStore.triggeredJobComplete();
	}

	@ApiOperation("发送过程信息")
	@PutMapping(path = "/progress")
	public void saveRuntimeUrlLog(@RequestBody ProgressEvent progressEvent) {
//		idcPlugin.getStatusService().fireProgressEvent(progressEvent);
	}

	@PutMapping("/complete")
	public void fireCompleteEvent(@RequestBody CompleteEvent event) {
		logger.info("fireCompleteEvent",event.getNodeJobId());
		JobInstanceStatus jobInstanceStatus =  event.getFinalStatus();
		JobMessage message;
		if(jobInstanceStatus==JobInstanceStatus.FINISHED)
		{
			message = FinishMessage.newMessage(event.getNodeJobId());
		}else if(jobInstanceStatus==JobInstanceStatus.FAILED)
		{
			message = FailMessage.newMessage(event.getNodeJobId());
		}
		else if(jobInstanceStatus==JobInstanceStatus.CANCLED)
		{
			message = CancelMessage.newMessage(event.getNodeJobId());
		}else{
			throw new RuntimeException("illegal message type:"+jobInstanceStatus+",instanceId:"+event.getInstanceId());
		}
		message.setMessage(event.getMessage());
		TaskEventPlugin.eventService(qs).send(message);
//		idcPlugin.getStatusService().fireCompleteEvent(event);
	}

	@ExceptionHandler({ Throwable.class })
	public ResponseEntity<String> exception(Exception e) {
		ResponseEntity<String> resp = new ResponseEntity<>("无法完成操作，服务器异常: " + e.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		return resp;
	}
}
