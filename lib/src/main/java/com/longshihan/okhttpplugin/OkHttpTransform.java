package com.longshihan.okhttpplugin;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.tools.r8.graph.S;
import com.android.utils.FileUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.gradle.api.Project;
import org.gradle.internal.impldep.org.apache.ivy.util.FileUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.Opcodes.ASM5;

public class OkHttpTransform extends Transform {
    Project project;
    private static final String TAG = "OkhttpPlugin";

    public OkHttpTransform(Project project) {
        this.project = project;

    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        long startTime=System.currentTimeMillis();
        if (!ConfigUtils.okhttpEnable){
            System.out.println(" Okhttpplugin 插件关闭 2");
            return;
        }else {
            System.out.println(" Okhttpplugin 打开");
        }
        try {
            Collection<TransformInput> inputs = transformInvocation.getInputs();
            TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
            inputs.forEach(input -> {
                input.getJarInputs().forEach(jarInput -> {
                        handleJarInputs(jarInput, outputProvider);
                });
                input.getDirectoryInputs().forEach(directoryInput -> {
                    handleDirectoryInput(directoryInput, outputProvider);
                });

            });
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }finally {
            System.out.println(" 插件耗时： "+(System.currentTimeMillis()-startTime)+"ms");
        }
    }

    /**
     * 处理Jar中的class文件
     */
    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider)  {
        File tmpFile = null;
        File dest = null;
        try {
            if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
                //重名名输出文件,因为可能同名,会覆盖
                String jarName = jarInput.getName();
                String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4);
                }
                dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                JarFile jarFile = new JarFile(jarInput.getFile());
                Enumeration enumeration = jarFile.entries();
                tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp" + jarName + ".jar");
                //避免上次的缓存被重复插入
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
                //用于保存

                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    String entryName = jarEntry.getName();
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    //插桩class
                    System.out.println("----------- deal with  class file <" + entryName + "> -----------");
                    if ("okhttp3/Request$Builder.class".equals(entryName)) {
                        //class文件处理
                        jarOutputStream.putNextEntry(zipEntry);
                        ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                        ClassVisitor cv = new OkHttpClassVisitor(ASM5, classWriter);
                        classReader.accept(cv, EXPAND_FRAMES);
                        byte[] code = classWriter.toByteArray();
                        jarOutputStream.write(code);
                    } else {
                        jarOutputStream.putNextEntry(zipEntry);
                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
                    }
                    jarOutputStream.closeEntry();
                }
                //结束
                jarOutputStream.close();
                jarFile.close();
                FileUtils.copyFile(tmpFile, dest);
                tmpFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    static void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        try {
            //是否为目录
            if (directoryInput.getFile().isDirectory()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.getFile(), dest);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
