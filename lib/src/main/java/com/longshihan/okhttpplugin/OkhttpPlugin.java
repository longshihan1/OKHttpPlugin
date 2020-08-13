package com.longshihan.okhttpplugin;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OkhttpPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        boolean isRoot= project.getPlugins().hasPlugin(AppPlugin.class);
        if (isRoot){
            ((BaseExtension)project.getExtensions().getByName("android")).registerTransform(new OkHttpTransform(project));
        }
    }
}
