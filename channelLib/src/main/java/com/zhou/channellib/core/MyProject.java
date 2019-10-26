package com.zhou.channellib.core;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MyProject implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        //创建gradle自定义信息
        project.getExtensions().create("channelExt", ChannelExt.class);

        //创建自定义任务，参数为名字以及实现类
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                project.getTasks().create("assembleChannelPkgs", MyTask.class);
            }
        });
    }
}
