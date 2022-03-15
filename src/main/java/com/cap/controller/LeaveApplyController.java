package com.cap.controller;

import com.cap.service.LeaveApplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(value = "请假申请流程接口", tags = "请假申请流程")
@RestController
public class LeaveApplyController {
    @Resource
    RepositoryService repositoryService;

    @Resource
    RuntimeService runtimeService;

    @Resource
    FormService formService;

    @Resource
    IdentityService identityService; //身份认证服务

    @Resource
    TaskService taskService;

    @Resource
    HistoryService historyService;

    @Resource
    ProcessEngineConfiguration configuration;

    @Resource
    LeaveApplyService leaveApplyService;

    //1.用户信息的绑定
    {
        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        IdentityService is = engine.getIdentityService();

        //添加用户组
        Group groupA = is.newGroup("groupA");
        is.saveGroup(groupA);
        //添加用户
        String id = "userIdA";
        User user = is.newUser(id);
        user.setPassword("123");
        is.saveUser(user);
        //绑定关系

    }

    //2.登录鉴权
    {
        //ACT_ID_USER ID_ PWD_
        String name = "";
        String password = "";
        //布尔值即可判断是否登录
        boolean result = identityService.checkPassword(name, password);
    }

    @ApiOperation("部署请假流程")
    @RequestMapping(value = "/deployWorkflow", method = RequestMethod.POST)
    public String deployWorkflow(@RequestParam MultipartFile uploadFile) {
        try {
            //部署并存储 流程文件bpmn png
            MultipartFile file = uploadFile;
            String filename = file.getOriginalFilename();
            InputStream is = file.getInputStream();
            repositoryService.createDeployment().addInputStream(filename, is).deploy();

            //用户组权限的绑定
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            RepositoryService rs = engine.getRepositoryService();
            //部署并存储 流程文件bpmn png
            Deployment dp = rs.createDeployment().addClasspathResource("xxx.bpmn").deploy();
            ProcessDefinition pd = rs.createProcessDefinitionQuery().deploymentId(dp.getId()).singleResult();
            rs.addCandidateStarterGroup(pd.getId(), "用户组");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "index";
    }

    //申请人发起审批
    @ApiOperation("发起请假申请")
    @RequestMapping(value = "/startLeaveApply", method = RequestMethod.POST)
    @ResponseBody
    public String startLeaveApply() {
        Map<String, Object> variables = new HashMap(100);
        variables.put("applicant", "zhangsan");
        variables.put("super", "lisi");
        variables.put("mgr", "wangwu");
        variables.put("days", "2");
        leaveApplyService.startWorkflow(variables);
        //更改业务表信息
        return "success";
    }

    //领导审批
    @ApiOperation("获取当前待办任务")
    @RequestMapping(value = "/getMyTodoTask", method = RequestMethod.POST)
    @ResponseBody
    public int getMyTodoTask(String username) {
        // 根据当前用户查询任务
/*        List<Task> tasks = taskService.createTaskQuery().list();
        for (Task task : tasks) {
            System.out.println(task.getName());
            System.out.println(task.getId());
            System.out.println(task.getProcessDefinitionId());

            // 查询待办任务并执行
            taskService.complete(task.getId());
        }*/
        //查询代办任务
        Task task = taskService.createTaskQuery().processInstanceId("").singleResult();
        HashMap<String, Object> map = new HashMap<>(16);
        //设定变量为result,result=1=>同意，result=0=>不同意
        map.put("result", 1);
        taskService.complete(task.getId(), map);

        return 0;
    }

    //会签审批 ->判断任务列表是否为null 为null的话就是执行完成的
    @ApiOperation("会签审批")
    @RequestMapping(value = "/multiApproval", method = RequestMethod.POST)
    @ResponseBody
    public void multiApproval() {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId("流程实例id").list();

    }

    @ApiOperation("查看工作流图片")
    @RequestMapping(value = "/showResource", method = RequestMethod.GET)
    public void showResource(@RequestParam("defId") String defId, @RequestParam("resource") String resource,
                             HttpServletResponse response) throws Exception {
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery().processDefinitionId(defId).singleResult();
        InputStream is = repositoryService.getResourceAsStream(def.getDeploymentId(), resource);
        ServletOutputStream output = response.getOutputStream();
        IOUtils.copy(is, output);
    }


    @ApiOperation("根据流程实例编号获取历史流程数据")
    @RequestMapping(value = "/getHistoryByInstId", method = RequestMethod.GET)
    @ResponseBody
    public List<HistoricActivityInstance> getHistoryByInstId(@RequestParam("instId") String instId) {
        List<HistoricActivityInstance> his = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instId).orderByHistoricActivityInstanceStartTime().asc().list();
        return his;
    }

    @ApiOperation("根据业务单号获取历史流程数据")
    @RequestMapping(value = "/getHistoryByBizNo", method = RequestMethod.GET)
    @ResponseBody
    public List<HistoricActivityInstance> getHistoryByBizNo(@RequestParam("bizNo") String bizNo) {
        String instId = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("leave_apply")
                .processInstanceBusinessKey(bizNo).singleResult().getId();
        List<HistoricActivityInstance> his = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instId).orderByHistoricActivityInstanceStartTime().asc().list();
        return his;
    }


}
