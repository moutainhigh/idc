package com.iwellmass.idc.app.vo;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iwellmass.idc.model.JobKey;

import io.swagger.annotations.ApiModelProperty;

public class ComplementRequest extends JobKey {

	private static final long serialVersionUID = 4804767124348623643L;

	@ApiModelProperty("开始时间，yyyyMMdd")
	@JsonFormat(pattern = "yyyyMMdd")
	private LocalDate startTime;

	@ApiModelProperty("截至时间，yyyyMMdd")
	@JsonFormat(pattern = "yyyyMMdd")
	private LocalDate endTime;

	public LocalDate getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDate startTime) {
		this.startTime = startTime;
	}

	public LocalDate getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDate endTime) {
		this.endTime = endTime;
	}

}