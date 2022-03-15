package com.cap.service.impl;

import com.cap.service.LeaveApplyService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@Service
public class LeaveApplyServiceImpl implements LeaveApplyService {
    @Autowired
    IdentityService identityService;
    @Autowired
    RuntimeService runtimeService;
    @Autowired
    TaskService taskService;

    @Override
    public ProcessInstance startWorkflow(Map<String, Object> variables) {
        // todo 先处理业务表单
        //获取用户信息
        String userId = "";
        //用户授权
        identityService.setAuthenticatedUserId(variables.get("applicant").toString());
        //启动流程 act_ru_execution START_USER_ID_
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("leave_apply bpmn id", variables);
        //填写任务 入参可以填写表单信息
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        //入参VO
        HashMap<String, Object> vo = new HashMap<>(16);
        taskService.claim(task.getId(), userId);//可有可无 方便查询
        taskService.complete(task.getId(), vo);//完成
        return instance;
    }

    @Override
    public List<ProcessInstance> listProcessFlow(String userId) {
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().startedBy(userId).list();
        return list;
    }


}
