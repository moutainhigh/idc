package com.iwellmass.idc.app.service;

import com.iwellmass.common.ServiceResult;
import com.iwellmass.idc.app.controller.InitController;
import com.iwellmass.idc.app.repo.TaskRepository;
import com.iwellmass.idc.model.Task;
import com.iwellmass.idc.model.TaskType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class InitService {

    @Inject
    private TaskRepository repository;

    public ServiceResult init(List<Task> tasks) {
        repository.save((Iterable<Task>) () -> tasks.iterator());
        if (tasks.size() == repository.countAll()) {
            // success
            return ServiceResult.success("添加成功");
        } else {
            //fail
            repository.delete(() -> tasks.iterator());
            return ServiceResult.failure("添加失败");
        }
    }
}