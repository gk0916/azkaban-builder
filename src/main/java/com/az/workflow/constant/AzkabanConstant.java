package com.az.workflow.constant;

/**
 * @author jingzhou
 */
public class AzkabanConstant {

    public static final String AZKABAN_REST_URL = "azkaban.rest.url";
    public static final String AZKABAN_USER_NAME = "azkaban.user.name";
    public static final String AZKABAN_PASSWORD = "azkaban.password";

    public static final String AZKABAN_ZIP_FILE_PATH = "azkaban.zip.path";

    public static final String LOGIN_API = "/?action=login";
    public static final String EXISTS_PROJECT_API = "/manager?ajax=existsProject";
    public static final String CREATE_PROJECT_API = "/manager?action=create";
    public static final String UPLOAD_FLOW_ZIP_API = "/manager?ajax=upload";
    public static final String EXECUTE_FLOW_API = "/executor?ajax=executeFlow";
    public static final String SCHEDULE_FLOW_API = "/schedule?ajax=scheduleCronFlow";
    public static final String CANCEL_FLOW_API = "/executor?ajax=cancelFlow";
    public static final String FETCH_EXEC_FLOW_API = "/executor?ajax=fetchexecflow";
    public static final String FETCH_EXEC_FLOW_LOGS_API = "/executor?ajax=fetchExecFlowLogs";
    public static final String FETCH_EXEC_JOB_LOGS_API = "/executor?ajax=fetchExecJobLogs";
    public static final String UNSCHEDULE_FLOW_API = "/schedule?action=removeSched";
    public static final String FETCH_FLOW_GRAPH_API = "/manager?ajax=fetchflowgraph";
    public static final String RESUME_FLOW_API = "/executor?ajax=resumeFlow";
    public static final String FETCH_RUNNING_EXECUTIONS_API = "/executor?ajax=getRunning";
    public static final String FETCH_SCHEDULE_API = "/schedule?ajax=fetchSchedule";
    public static final String FETCH_PROJECT_FLOWS_API = "/manager?ajax=fetchprojectflows";
    public static final String GET_PROJECT_API = "/manager?ajax=fetchprojectflows";

    // Flow 2.0 node type
    public static final String JOB_NODE_TYPE = "command";
    public static final String FLOW_NODE_TYPE = "flow";

    public static final String JOB_CONFIG_KEY = "command";

    public static final String FLOW_VERSION_INFO_FILE = "azkaban.project";
    public static final String FLOW_VERSION_INFO = "azkaban-flow-version: 2.0";

}
