# azkaban-builder
azkaban DAG 构建工具
## examples
```
        Map workflow_params = new LinkedHashMap();
        workflow_params.put("param1","azkaban");

        Map job1_params = new LinkedHashMap();
        job1_params.put("job_param","job1_param");

        Map job2_params = new LinkedHashMap();
        job2_params.put("job_param","job2_param");


        AzkabanJob job1 = AzkabanJob.builder()
                .name("job1")
                .commands(Arrays.asList("echo hello ${param1}, running job1 with ${job_param}"))
                .dependsOn(Arrays.asList())
                .config(job1_params)
                .bulid();

        AzkabanJob job2 = AzkabanJob.builder()
                .name("job2")
                .commands(Arrays.asList("echo hello ${param1}, running job2 with ${job_param}"))
                .dependsOn(Arrays.asList(job1.getName()))
                .config(job2_params)
                .bulid();

        AzkabanWorkflow workflow = AzkabanWorkflow.builder()
                .projectName("test_running")
                .name("flow1")
                .condition("all_success")
                .config(workflow_params)
                .cron("0 0/1 * * * ?")
                .jobs(Arrays.asList(job1,job2).stream().collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();

        IProfileService profileService = new AzkabanProfileService();
        profileService.submit(workflow);
        profileService.schedule(workflow.getProjectName(),workflow.getName(),workflow.getCronExpression());