package com.longshihan.okhttpplugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class OkHttpClassVisitor extends ClassVisitor {
    public OkHttpClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
       MethodVisitor mv= super.visitMethod(access, name, descriptor, signature, exceptions);
       mv=new AdviceAdapter(Opcodes.ASM5,mv,access,name,descriptor) {
           @Override
           protected void onMethodEnter() {
               if (name.equals("build")) {
                   mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/dada/mobile/android/security/DSecurity",
                           "stacktrace", "()V", false
                   );
                   super.onMethodEnter();
               } else {
                   System.out.println("== onMethodEnter, owner = +, name =" + name);
                   super.onMethodEnter();
               }
           }
       };
       return mv;
    }
}
