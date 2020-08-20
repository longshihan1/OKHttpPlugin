package com.longshihan.okhttpplugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OkhttpPlugin implements Plugin<Project> {
    DadaConfig config=new DadaConfig();

    @Override
    public void apply(Project project) {
       config= project.getExtensions().create("dadaconfig",DadaConfig.class);
        boolean isRoot = project.getPlugins().hasPlugin(AppPlugin.class);
        if (isRoot) {
            AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
            project.afterEvaluate(new Action<Project>() {
                @Override
                public void execute(Project project) {
                    if (config == null) {
                        config = (DadaConfig) project.getExtensions().getByName("dadaconfig");
                    }
                    if (config.okhttpPluginEnable) {
                        ConfigUtils.okhttpEnable=true;
                        System.out.println(" Okhttpplugin 插件配置开启 ");
                    } else {
                        ConfigUtils.okhttpEnable=false;
                        System.out.println(" Okhttpplugin 插件配置关闭 ");
                    }
                }
            });
            ((BaseExtension) project.getExtensions().getByName("android")).registerTransform(new OkHttpTransform(project));
        }
    }
}
