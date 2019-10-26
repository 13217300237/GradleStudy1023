package com.zhou.channellib.core;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class MyProject implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        //����gradle�Զ�����Ϣ
        project.getExtensions().create("channelExt", ChannelExt.class);

        //�����Զ������񣬲���Ϊ�����Լ�ʵ����
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                project.getTasks().create("assembleChannelPkgs", MyTask.class);
            }
        });
    }
}
