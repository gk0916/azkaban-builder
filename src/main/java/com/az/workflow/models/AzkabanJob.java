package com.az.workflow.models;

import java.util.*;

/**
 * @author jingzhou
 */
public class AzkabanJob extends Job {
    private String workflowId;
    private String name;
    private List<String> commands;
    private List<String> files;
    private List<String> dependencies;
    private Map config;
    private String condition;

    public AzkabanJob(Properties properties) {
        this.workflowId = properties.getProperty("workflow_id");
        this.name = properties.getProperty("name");
        this.commands = (List<String>) properties.get("commands");
        this.dependencies = (List<String>) properties.get("depends");
        this.files = (List<String>) properties.get("files");
        this.config = (Map) properties.get("config");
        this.condition = (String) properties.get("condition");
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
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

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AzkabanJob){
            return ((AzkabanJob) obj).name == this.name;
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Properties properties;

        public Builder() {
            properties = new Properties();
            properties.put("files", new ArrayList<>());
            properties.put("depends", new ArrayList<>());
            properties.put("config", new HashMap<>());
        }

        public Builder worflowId(String workflowId) {
            properties.put("workflow_id", workflowId);
            return this;
        }

        public Builder name(String name) {
            properties.put("name", name);
            return this;
        }

        public Builder commands(List<String> commands) {
            properties.put("commands", commands);
            return this;
        }

        public Builder dependsOn(List<String> depends) {
            properties.put("depends", depends == null ? new ArrayList<>() : depends);
            return this;
        }

        public Builder files(List<String> files) {
            properties.put("files", files == null ? new ArrayList<>() : files);
            return this;
        }

        public Builder config(Map configs) {
            properties.put("config", configs == null ? new HashMap<>() : configs);
            return this;
        }

        public Builder condition(String condition) {
            properties.put("condition", condition == null ? "all_success" : condition);
            return this;
        }

        public AzkabanJob bulid() {
            return new AzkabanJob(properties);
        }
    }
}
