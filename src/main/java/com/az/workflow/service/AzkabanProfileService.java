package com.az.workflow.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.az.workflow.constant.AzkabanConstant;
import com.az.workflow.exception.ServerException;
import com.az.workflow.models.AzkabanJob;
import com.az.workflow.models.AzkabanWorkflow;
import com.az.workflow.models.Workflow;
import com.az.workflow.utils.HttpUtil;
import com.az.workflow.utils.PropsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author gk
 */
public class AzkabanProfileService implements IProfileService {
    private static final String FILE_EXTENSION = ".zip";
    private static final String FLOW_FILE_EXTENSION = ".flow";
    Properties prop = PropsUtil.loadProps("workflow.properties");
    String restURL = PropsUtil.getString(prop, AzkabanConstant.AZKABAN_REST_URL);
    private static final Logger logger = LoggerFactory.getLogger(AzkabanProfileService.class);

    @Override
    public Boolean existsProject(String projectName) {
        // 登录Azkaban，返回session.id信息
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String result = existsProjectByApi(sessionId, projectName);
        JSONObject jsonObject = JSON.parseObject(result);
        if (jsonObject.containsKey("exists")) {
            return jsonObject.getBoolean("exists");
        }
        return false;
    }

    @Override
    public Boolean createProject(String projectName, String description) {
        // 登录Azkaban，返回session.id信息
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        // 创建
        JSONObject jsonObject = JSON.parseObject(creatProjectByApi(sessionId, projectName, description));
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5000", "创建azkaban项目失败：" + jsonObject.getString("message"));
        } else if (jsonObject.containsKey("status") && jsonObject.getString("status").equals("error")) {
            throw new ServerException("5000", "创建azkaban项目失败：" + jsonObject.getString("message"));
        }
        return true;
    }

    @Override
    public Boolean submit(Workflow workflow) {
        if(! (workflow instanceof AzkabanWorkflow)){
            throw new ServerException("","");
        }
        AzkabanWorkflow AzWorkflow = (AzkabanWorkflow) workflow;
        if (workflow == null) {
            logger.error("AzkabanProfileService: workflow为空！");
            throw new ServerException("5001", "参数异常");
        }
        Boolean response = submit(AzWorkflow.getProjectName(), Arrays.asList(workflow));
        return response;
    }

    @Override
    public Boolean submit(String projectName, List<Workflow> workflows) {
        if (projectName == null) {
            logger.error("AzkabanProfileService: 项目名为空！");
            throw new ServerException("5001", "参数异常");
        }
        if (workflows == null || workflows.isEmpty()) {
            logger.error("AzkabanProfileService: workflows为空！");
            throw new ServerException("5001", "参数异常");
        }

//        if (!existsProject(projectName)) {
//            createProject(projectName, projectName + " info");
//        }


        Map<Map, Set<String>> map = new HashMap<>(workflows.size() + 1);
        for (Workflow workflow : workflows) {
            if(!(workflow instanceof AzkabanWorkflow)){
                throw new ServerException("", "");
            }
            AzkabanWorkflow azWorkflow = (AzkabanWorkflow) workflow;
            if (workflows == null) {
                logger.error("AzkabanProfileService: workflow为空！");
                throw new ServerException("5001", "参数异常");
            }
            // 生成Azkaban工作流配置文件
            Set<String> resourceFiles = new HashSet<>();
            Map nodeBean = flow2NodeBean(azWorkflow, resourceFiles);
            map.put(nodeBean, resourceFiles);
        }
        // 打包文件为zip包
        String zipFile = PropsUtil.getString(prop, AzkabanConstant.AZKABAN_ZIP_FILE_PATH) +
                projectName + FILE_EXTENSION;
        packageZip(map, zipFile);

//        // 登录Azkaban，返回session.id信息
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        // 上传zip包到Azkaban系统
        String response = uploadWorkflowZipByApi(sessionId, projectName, zipFile);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5002", "上传zip文件失败：" + jsonObject.getString("error"));
        }
        return true;
    }

    @Override
    public String execute(String projectName, String workflowName) {
        // 登录Azkaban，返回session.id信息
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        // 执行
        JSONObject jsonObject = JSON.parseObject(executeFlowByApi(sessionId, projectName, workflowName));
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5003", "运行工作流失败：" + jsonObject.getString("error"));
        }
        return jsonObject.getString("execid");
    }

    @Override
    public Boolean schedule(String projectName, String workflowName, String cronExpression) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = scheduleFlowByApi(sessionId, projectName, workflowName, cronExpression);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("status") && jsonObject.getString("status").equals("error")) {
            logger.error("");
            throw new ServerException("5004", "调度工作流失败：" + jsonObject.getString("message"));
        }
        return true;
    }

    @Override
    public Boolean cancel(String execId) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = cancelFlowByApi(sessionId, execId);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5005", "停止工作流失败：" + jsonObject.getString("error"));
        }
        return true;
    }

    @Override
    public String execStatus(String execId) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = execStatusByApi(sessionId, execId);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5006", "查看工作流运行状态失败：" + jsonObject.getString("error"));
        }
        return response;
    }

    @Override
    public String execFlowLogs(String execId) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = execFlowLogsByApid(sessionId, execId);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5007", "查看工作流运行日志失败：" + jsonObject.getString("error"));
        }
        return jsonObject.getString("data");
    }

    @Override
    public String execFlowJobLogs(String execId, String jobId, String offset, String length) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = execFlowJobLogsByApid(sessionId, execId, jobId, offset, length);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("5008", "查看任务运行日志失败：" + jsonObject.getString("error"));
        }
        return jsonObject.getString("data");
    }

    @Override
    public Boolean unSchedule(String project, String flow) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));

        JSONObject flows = fetchProjectFlows(project);
        String projectId = null;
        if (flows.containsKey("projectId")) {
            projectId = flows.getString("projectId");
        } else {
            throw new ServerException("5014", "此项目下没有工作流：" + project);
        }

        JSONObject schedule = fetchSchedule(projectId, flow);
        String scheduleId = null;
        if (schedule.containsKey("schedule")) {
            schedule = JSONObject.parseObject(schedule.getString("schedule"));
            if (schedule.containsKey("scheduleId")) {
                scheduleId = schedule.getString("scheduleId");
            } else {
                throw new ServerException("5015", "没有找到scheduleId，请检查是否配置了定时调度！");
            }
        } else {
            throw new ServerException("5016", "此工作流没有调度属性：" + flow);
        }

        String response = unScheduleFlowByApi(sessionId, scheduleId);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (jsonObject.containsKey("status") && jsonObject.getString("status").equals("error")) {
            throw new ServerException("5009", "取消调度工作流失败：" + jsonObject.getString("message"));
        }
        return true;
    }

    @Override
    public Object fetchflowGraph(String project, String flow) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = fetchflowGraphByApi(sessionId, project, flow);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("50010", "fetchflowgraph失败：" + jsonObject.getString("error"));
        }
        return jsonObject;
    }

    @Override
    public Object resumeFlow(String execid) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = resumeFlowByApi(sessionId, execid);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("50011", "resumeFlow失败：" + jsonObject.getString("error"));
        }
        return jsonObject;
    }

    @Override
    public JSONObject fetchProjectFlows(String project) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = fetchProjectFlowsByApi(sessionId, project);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("50012", "fetchProjectFlows失败：" + jsonObject.getString("error"));
        }
        return jsonObject;
    }

    @Override
    public JSONObject fetchSchedule(String projectId, String flow) {
        String sessionId = loginByApi(PropsUtil.getString(prop, AzkabanConstant.AZKABAN_USER_NAME),
                PropsUtil.getString(prop, AzkabanConstant.AZKABAN_PASSWORD));
        String response = fetchScheduleByApi(sessionId, projectId, flow);
        JSONObject jsonObject = JSON.parseObject(response);
        if (jsonObject.containsKey("error")) {
            throw new ServerException("50013", "fetchSchedule失败：" + jsonObject.getString("error"));
        }
        return jsonObject;
    }

    @Override
    public String login(String userName, String password) {
        return loginByApi(userName,password);
    }

    /**
     * workflow信息转换成Azkaban工作流配置文件,打包成zip包
     */
    public Map<String, Object> transferWorkflow(AzkabanWorkflow workflow) {
        Map<String, Object> map = new HashMap<>();
        Set<String> resourceFiles = new HashSet<>();
        Map nodeBean = flow2NodeBean(workflow, resourceFiles);
        map.put("nodeBean", nodeBean);
        map.put("resourceFiles", resourceFiles);
        return map;
    }

    /**
     * 转换AzkabanWorkflow到NodeBean
     *
     * @param workflow
     * @param resouce
     * @return
     */
    public Map flow2NodeBean(AzkabanWorkflow workflow, Set<String> resouce) {
        Map nodeMap = new LinkedHashMap();
        nodeMap.put("name",workflow.getName());
        nodeMap.put("type",AzkabanConstant.FLOW_NODE_TYPE);
        nodeMap.put("config",workflow.getConfig());
        nodeMap.put("condition",workflow.getCondition());
        nodeMap.put("dependsOn",workflow.getDependencies());
        List nodes = workflow.getSubFlows().stream()
                .filter(flow->flow!=null)
                .map(subFlow -> flow2NodeBean(subFlow,resouce))
                .collect(Collectors.toList());
        nodes.addAll(workflow.getJobs().stream()
                .filter(job->job!=null)
                .map(job->job2NodeBean(job,resouce))
                .collect(Collectors.toList()));
        nodeMap.put("trigger",null);
        nodeMap.put("nodes",nodes);
//        NodeBean nodeBean = new NodeBean();
//        nodeBean.setName(workflow.getName());
//        nodeBean.setType(AzkabanConstant.FLOW_NODE_TYPE);
//        nodeBean.setDependsOn(workflow.getDependencies());
//        nodeBean.setConfig(workflow.getConfig());
//        nodeBean.setCondition(workflow.getCondition());
//        nodeBean.setNodes(workflow.getSubFlows()
//                .stream()
//                .filter(flow -> flow != null)
//                .map(subFlow -> flow2NodeBean(subFlow, resouce))
//                .collect(Collectors.toList()));
//        nodeBean.getNodes().addAll(workflow.getJobs()
//                .stream()
//                .filter(job -> job != null)
//                .map(job -> job2NodeBean(job, resouce))
//                .collect(Collectors.toList()));
        return nodeMap;
    }

    /**
     * 转换AzkabanJob到NodeBean
     * @param job
     * @return
     */
    private Map job2NodeBean(AzkabanJob job, Set<String> resources) {
        List<String> files = job.getFiles();
        if (files != null && files.size() > 0) {
            resources.addAll(job.getFiles()
                    .stream()
                    .filter(file -> file != null)
                    .collect(Collectors.toList()));
        }
        Map nodeMap = new LinkedHashMap();
        nodeMap.put("name",job.getName());
        nodeMap.put("type",AzkabanConstant.JOB_NODE_TYPE);
        Map config = job.getConfig();
        List<String> commands = job.getCommands();
        if(commands.size()>1){
            config.put(AzkabanConstant.JOB_CONFIG_KEY,commands.get(0));
            for (int i = 1; i < commands.size(); i++) {
                config.put(AzkabanConstant.JOB_CONFIG_KEY+"."+i,commands.get(i));
            }
        } else {
          config.put(AzkabanConstant.JOB_CONFIG_KEY,commands.get(0));
        }
//        job.getCommands().stream().forEach(command -> {
//            config.put(AzkabanConstant.JOB_CONFIG_KEY, command);
//        });
        nodeMap.put("config",config);
        nodeMap.put("condition",job.getCondition());
        nodeMap.put("dependsOn",job.getDependencies());
        nodeMap.put("trigger",null);
//        NodeBean nodeBean = new NodeBean();
//        nodeBean.setName(job.getName());
//        nodeBean.setType(AzkabanConstant.JOB_NODE_TYPE);
//        nodeBean.setDependsOn(job.getDependencies());
//        nodeBean.setCondition(job.getCondition());
//        Map map = new HashMap<String, String>();
//        AtomicInteger num = new AtomicInteger(1);
//        job.getCommands().stream().forEach(command -> {
//            map.put(AzkabanConstant.JOB_CONFIG_KEY + "." + num, command);
//            num.addAndGet(1);
//        });
//        nodeBean.setConfig(map);

        return nodeMap;
    }

    /**
     * 压缩文件
     */
    public static boolean packageZip(Map<Map, Set<String>> nodeBeanListMap, String zipFile) {
        ZipOutputStream zipStream = null;
        FileInputStream zipSource = null;
        BufferedInputStream bufferStream = null;
        try {
            // zip包添加azkaban.project文件
            zipStream = new ZipOutputStream(new FileOutputStream(zipFile));
            ZipEntry versionInfoEntry = new ZipEntry(AzkabanConstant.FLOW_VERSION_INFO_FILE);
            zipStream.putNextEntry(versionInfoEntry);
            zipStream.write(AzkabanConstant.FLOW_VERSION_INFO.getBytes());

            for (Map.Entry<Map, Set<String>> entry : nodeBeanListMap.entrySet()) {
                Map nodeBean = entry.getKey();
                Set<String> resourcesFiles = entry.getValue();
                // 添加flow文件
                ZipEntry flowEntry = new ZipEntry(nodeBean.get("name") + FLOW_FILE_EXTENSION);
                zipStream.putNextEntry(flowEntry);
                Yaml yaml = new Yaml();
                String dump = yaml.dump(nodeBean);
                zipStream.write(dump.getBytes());
                logger.info(zipFile);
                logger.info("=============================>\n{}<=============================",dump);

                // 添加当前flow所需的资源文件
                for (String source : resourcesFiles) {
                    // File file = downLoadFromUrl(source, "/tmp/", "");
                    File file = new File(source);
                    if (!file.exists()) {
                        logger.error("找不到文件: {}", file.getPath());
                        throw new ServerException("50020", "文件不存在！");
                    }
                    zipSource = new FileInputStream(file);
                    byte[] bufferArea = new byte[1024 * 10];
                    /** 压缩条目不是具体独立的文件，而是压缩包文件列表中的列表项，称为条目，就像索引一样。
                    **  这里以文件名命名，此时，任务command中的文件路径也要相应地指定为文件名 */
//                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    ZipEntry zipEntry = new ZipEntry(file.getPath());
                    // 定位到该压缩条目位置，开始写入文件到压缩包中
                    zipStream.putNextEntry(zipEntry);
                    bufferStream = new BufferedInputStream(zipSource, 1024 * 10);
                    int read = 0;
                    while ((read = bufferStream.read(bufferArea, 0, 1024 * 10)) != -1) {
                        zipStream.write(bufferArea, 0, read);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // 关闭流
            try {
                if (null != bufferStream) {
                    bufferStream.close();
                }
                if (null != zipStream) {
                    zipStream.close();
                }
                if (null != zipSource) {
                    zipSource.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 调用Azkaban接口，登录Azkaban系统
     */
    public String loginByApi(String userName, String password) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("action", "login");
        params.add("username", userName);
        params.add("password", password);
        String url = restURL + AzkabanConstant.LOGIN_API;
        // 调用Azkaban的登录api，返回session.id信息
        String resultStr = HttpUtil.request(params, url);
        JSONObject jsonObject = JSON.parseObject(resultStr);
        if (jsonObject.containsKey("session.id")) {
            return jsonObject.getString("session.id");
        }
        throw new ServerException("50021", "azkaban登录异常");
    }

    /**
     * 调用Azkaban接口，是否已存在azkaban项目
     */
    private String existsProjectByApi(String sessionId, String projectName) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("session.id", sessionId);
        params.add("ajax", "existsProject");
        params.add("project", projectName);
        String url = restURL + AzkabanConstant.EXISTS_PROJECT_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，创建azkaban项目
     */
    private String creatProjectByApi(String sessionId, String projectName, String description) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("session.id", sessionId);
        params.add("action", "create");
        params.add("name", projectName);
        params.add("description", description);
        String url = restURL + AzkabanConstant.CREATE_PROJECT_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，上传zip文件到Azkaban系统
     */
    private String uploadWorkflowZipByApi(String sessionId, String project, String filePath) {
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        HttpHeaders header = new HttpHeaders();
        header.setContentType(type);

        FileSystemResource fileSystemResource = new FileSystemResource(filePath);
        // 传递参数
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("session.id", sessionId);
        params.add("ajax", "upload");
        params.add("project", project);
        params.add("file", fileSystemResource);
        String url = restURL + AzkabanConstant.UPLOAD_FLOW_ZIP_API;

        return HttpUtil.request(header, params, url);
    }

    /**
     * 调用Azkaban接口，执行工作流
     */
    private String executeFlowByApi(String sessionId, String project, String flow) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "executeFlow");
        params.add("session.id", sessionId);
        params.add("project", project);
        params.add("flow", flow);
        String url = restURL + AzkabanConstant.EXECUTE_FLOW_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，调度工作流
     */
    private String scheduleFlowByApi(String sessionId, String project, String flow, String cronExpression) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "scheduleCronFlow");
        params.add("session.id", sessionId);
        params.add("projectName", project);
        params.add("flow", flow);
        params.add("cronExpression", cronExpression);
        String url = restURL + AzkabanConstant.SCHEDULE_FLOW_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，工作流取消执行
     */
    private String cancelFlowByApi(String sessionId, String execId) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "cancelFlow");
        params.add("session.id", sessionId);
        params.add("execid", execId);
        String url = restURL + AzkabanConstant.CANCEL_FLOW_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，获取工作流执行状态
     */
    private String execStatusByApi(String sessionId, String execId) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchexecflow");
        params.add("session.id", sessionId);
        params.add("execid", execId);
        String url = restURL + AzkabanConstant.FETCH_EXEC_FLOW_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，获取工作流运行日志
     */
    public String execFlowLogsByApid(String sessionId, String execId) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchExecFlowLogs");
        params.add("session.id", sessionId);
        params.add("execid", execId);
        params.add("offset", "0");
        params.add("length", "50000");
        String url = restURL + AzkabanConstant.FETCH_EXEC_FLOW_LOGS_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，获取任务运行日志
     */
    private String execFlowJobLogsByApid(String sessionId, String execId,
                                         String jobId, String offset, String length) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchExecJobLogs");
        params.add("session.id", sessionId);
        params.add("execid", execId);
        params.add("jobId", jobId);
        params.add("offset", offset);
        params.add("length", length);
        String url = restURL + AzkabanConstant.FETCH_EXEC_JOB_LOGS_API;

        return HttpUtil.request(params, url);
    }

    /**
     * 调用Azkaban接口，取消调度工作流
     */
    private String unScheduleFlowByApi(String sessionId, String scheduleId) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("action", "removeSched");
        params.add("session.id", sessionId);
        params.add("scheduleId", scheduleId);
        String url = restURL + AzkabanConstant.UNSCHEDULE_FLOW_API;

        return HttpUtil.request(params, url);
    }

    private String fetchflowGraphByApi(String sessionId, String project, String flow) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchflowgraph");
        params.add("session.id", sessionId);
        params.add("project", project);
        params.add("flow", flow);
        String url = restURL + AzkabanConstant.FETCH_FLOW_GRAPH_API;

        return HttpUtil.request(params, url);
    }

    private String resumeFlowByApi(String sessionId, String execid) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "resumeFlow");
        params.add("session.id", sessionId);
        params.add("execid", execid);
        String url = restURL + AzkabanConstant.RESUME_FLOW_API;

        return HttpUtil.request(params, url);
    }

    private String fetchRunningExecutionsByApi(String sessionId, String project, String flow) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "getRunning");
        params.add("session.id", sessionId);
        params.add("project", project);
        params.add("flow", flow);
        String url = restURL + AzkabanConstant.FETCH_RUNNING_EXECUTIONS_API;

        return HttpUtil.request(params, url);
    }

    /**
     * Given a project id and a flow id, this API call fetches the schedule.
     *
     * @param sessionId The user session id.
     * @param projectId The id of the project.
     * @param flow      The name of the flow.
     * @return
     */
    public String fetchScheduleByApi(String sessionId, String projectId, String flow) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchSchedule");
        params.add("session.id", sessionId);
        params.add("projectId", projectId);
        params.add("flowId", flow);
        String url = restURL + AzkabanConstant.FETCH_SCHEDULE_API;

        return HttpUtil.request(params, url);
    }

    /**
     * Given a project name, this API call fetches all flow ids of that project.
     *
     * @param sessionId The user session id.
     * @param project   The project name to be fetched.
     * @return
     */
    public String fetchProjectFlowsByApi(String sessionId, String project) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "fetchprojectflows");
        params.add("session.id", sessionId);
        params.add("project", project);
        String url = restURL + AzkabanConstant.FETCH_PROJECT_FLOWS_API;

        return HttpUtil.request(params, url);
    }

    public String getProjectByApi(String sessionId, String project) {
        // 传递参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ajax", "getProject");
        params.add("session.id", sessionId);
        params.add("project", project);
        String url = restURL + AzkabanConstant.GET_PROJECT_API;

        return HttpUtil.request(params, url);
    }

    public static File downLoadFromUrl(String urlStr, String savePath, String toekn) throws IOException {
        if (StringUtils.isEmpty(urlStr)) {
            throw new ServerException("50022", "下载链接为空!");
        }
        URL url = new URL(urlStr);
        int index = url.getFile().lastIndexOf("/");
        String fileName = index < 0 ? url.getFile() : url.getFile().substring(index + 1);
        if (StringUtils.isEmpty(fileName)) {
            throw new ServerException("50023", "文件名为空！");
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //设置超时间为3秒
        conn.setConnectTimeout(3 * 1000);
        //防止屏蔽程序抓取而返回403错误
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        // conn.setRequestProperty("lfwywxqyh_token",toekn);

        //得到输入流
        InputStream inputStream = conn.getInputStream();
        //获取自己数组
        byte[] getData = readInputStream(inputStream);

        //文件保存位置
        File saveDir = new File(savePath);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }
        File file = new File(saveDir + File.separator + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(getData);
        if (fos != null) {
            fos.close();
        }
        if (inputStream != null) {
            inputStream.close();
        }
        logger.info(url + " download success");
        return file;
    }

    /**
     * 从输入流中获取字节数组
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }
}
