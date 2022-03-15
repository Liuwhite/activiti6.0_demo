package com.cap.service;


import org.activiti.engine.runtime.ProcessInstance;

import java.util.List;
import java.util.Map;


/**
 * @author zhuqm
 */
public interface LeaveApplyService {
    /**
     * 启动流程
     *
     * @param variables
     * @return
     */
    ProcessInstance startWorkflow(Map<String, Object> variables);

    /**
     * 查看当前人所属流程列表
     * @param userId
     * @return
     */
    List<ProcessInstance> listProcessFlow(String userId);
}