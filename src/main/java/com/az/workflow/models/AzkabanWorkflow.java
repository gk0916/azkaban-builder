package com.az.workflow.models;

import java.util.*;

/**
 * @author jingzhou
 */
public class AzkabanWorkflow extends Workflow {
    private String id;
    private String name;
    private String projectId;
    private String projectName;
    private String cronExpression;
    private LinkedHashSet<AzkabanJob> jobs;
    private List<String> dependencies;
    private List<AzkabanWorkflow> subFlows;
    private Map config;
    private String condition;

    private AzkabanWorkflow(Properties properties) {
        this.id = (String) properties.get("id");
        this.name = (String) properties.get("name");
        this.projectId = (String) properties.get("project_id");
        this.projectName = (String) properties.get("project_name");
        this.cronExpression = (String) properties.get("cron");
        this.jobs = (LinkedHashSet<AzkabanJob>) properties.get("jobs");
        this.dependencies = (List<String>) properties.get("depends");
        this.subFlows = (List<AzkabanWorkflow>) properties.get("sub_flows");
        this.config = (Map) properties.get("config");
        this.condition = (String) properties.get("condition");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public LinkedHashSet<AzkabanJob> getJobs() {
        return jobs;
    }

    public void setJobs(LinkedHashSet<AzkabanJob> jobs) {
        this.jobs = jobs;
    }

    public void addJobs(List<AzkabanJob> jobs) {
        this.jobs.addAll(jobs);
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependencies(List<String> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    public List<AzkabanWorkflow> getSubFlows() {
        return subFlows;
    }

    public void setSubFlows(List<AzkabanWorkflow> subFlows) {
        this.subFlows = subFlows;
    }

    public void addSubFlows(List<AzkabanWorkflow> workflows) {
        this.subFlows.addAll(workflows);
    }

    public Map getConfig() {
        return config;
    }

    public void setConfig(Map config) {
        this.config = config;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Properties properties;

        public Builder() {
            properties = new Properties();
            properties.put("jobs", new LinkedHashSet<>());
            properties.put("depends", new ArrayList<>());
            properties.put("sub_flows", new ArrayList<>());
            properties.put("config", new HashMap<>());
        }

        public Builder id(String id) {
            properties.put("id", id);
            return this;
        }

        public Builder name(String name) {
            properties.put("name", name);
            return this;
        }

        public Builder projectId(String projectId) {
            properties.put("project_id", projectId);
            return this;
        }

        public Builder projectName(String projectName) {
            properties.put("project_name", projectName);
            return this;
        }

        public Builder cron(String cronExpression) {
            properties.put("cron", cronExpression);
            return this;
        }

        public Builder jobs(LinkedHashSet<AzkabanJob> jobs) {
            properties.put("jobs", jobs == null ? new LinkedHashSet<>() : jobs);
            return this;
        }

        public Builder dependsOn(List<String> depends) {
            properties.put("depends", depends == null ? new ArrayList<>() : depends);
            return this;
        }

        public Builder subFlows(List<AzkabanWorkflow> subFlows) {
            properties.put("sub_flows", subFlows == null ? new ArrayList<>() : subFlows);
            return this;
        }

        public Builder config(Map configs) {
            properties.put("config", configs == null ? new HashMap<>(16) : configs);
            return this;
        }

        public Builder condition(String condition) {
            properties.put("condition", condition == null ? "all_success" : condition);
            return this;
        }

        public AzkabanWorkflow build() {
            AzkabanWorkflow workflow = new AzkabanWorkflow(properties);
            return workflow;
        }
    }
}
