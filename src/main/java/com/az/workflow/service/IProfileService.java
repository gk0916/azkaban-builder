package com.az.workflow.service;

import com.alibaba.fastjson.JSONObject;
import com.az.workflow.models.Workflow;

import java.util.List;

/**
 * @author gk
 */
public interface IProfileService {

    Boolean existsProject(String projectName);

    Boolean createProject(String projectName,String description);

    Boolean submit(Workflow workflow);

    Boolean submit(String projectName, List<Workflow> workflows);

    String execute(String projectName,String workflowName);

    Boolean schedule(String projectName,String workflowName,String cronExpression);

    Boolean cancel(String execId);

    String execStatus(String execId);

    String execFlowLogs(String execId);

    String execFlowJobLogs(String execId,String jobId,String offset,String length);

    Boolean unSchedule(String project, String flow);

    Object fetchflowGraph(String project,String flow);

    Object resumeFlow(String execid);

    JSONObject fetchProjectFlows(String project);

    JSONObject fetchSchedule(String projectId,String flow);

    String login(String userName, String password);
}
