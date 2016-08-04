/*
 * Copyright 2016 Danny Althoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal;

import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.gradle.api.GradleException;
import org.objectweb.asm.*;

/**
 *
 * @author Danny Althoff
 */
public class MonkeyPatcher {

    private static final String METHOD_TO_MONKEY_PATCH = "copyMSVCDLLs";
    private static final String METHOD_SIGNATURE_TO_MONKEY_PATCH = "(Ljava/io/File;Ljava/io/File;)V";
    private static final String FAULTY_CLASSFILE_TO_MONKEY_PATCH = "com/oracle/tools/packager/windows/WinAppBundler.class";

    public URL getPatchedJfxAntJar() throws MalformedURLException {
        String jfxAntJarPath = "/../lib/" + JavaFXGradlePlugin.ANT_JAVAFX_JAR_FILENAME;

        // on java 9, we have a different path
        if( JavaDetectionTools.IS_JAVA_9 ){
            jfxAntJarPath = "/lib/" + JavaFXGradlePlugin.ANT_JAVAFX_JAR_FILENAME;
        }

        File jfxAntJar = new File(System.getProperty("java.home") + jfxAntJarPath);

        if( !jfxAntJar.exists() ){
            throw new GradleException("Couldn't find Ant-JavaFX-library, please make sure you've installed some JDK which includes JavaFX (e.g. OracleJDK or OpenJDK and OpenJFX), and JAVA_HOME is set properly.");
        }

        // open jar-file as inputstream
        // copy into new jar-file into temp-folder
        // when found special WinAppBundler-file, process this file via ASM
        // generate new class-file
        // write that generated class-file instead of original file to new jar-file
        // return path to that file
        try{
            Path tempDirectory = Files.createTempDirectory("javafx-gradle-plugin-workaround");
            // delete that crap after JVM being shut down
            tempDirectory.toFile().deleteOnExit();

            JarFile jarFile = new JarFile(jfxAntJar, false, JarFile.OPEN_READ);

            try(FileOutputStream processedAntJar = new FileOutputStream(tempDirectory.resolve(JavaFXGradlePlugin.ANT_JAVAFX_JAR_FILENAME).toAbsolutePath().toFile())){
                ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(processedAntJar, StandardCharsets.UTF_8);
                jarFile.stream().forEachOrdered(jarEntry -> {
                    System.out.println("copying entry > " + jarEntry.getName());
                    ZipEntry zipEntry = new ZipEntry(jarEntry.getName());
                    try{
                        zipOutputStream.putNextEntry(zipEntry);

                        if( jarEntry.getName().equals(FAULTY_CLASSFILE_TO_MONKEY_PATCH) ){
                            System.out.println("WE FOUND OUR BUGGY THING");
                            // TODO patch jar-file

                            if( true == false ){
                                ClassReader classReader = new ClassReader(jarFile.getInputStream(jarEntry));

                                classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                                    @Override
                                    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                                        if( !(name.equals(METHOD_TO_MONKEY_PATCH) && desc.equals(METHOD_SIGNATURE_TO_MONKEY_PATCH)) ){
                                            return super.visitMethod(access, name, desc, signature, exceptions);
                                        }

                                        System.out.println("WE FOUND OUR BUGGY THING");

                                        return monkeyPatch_WinAppBundler();
                                    }

                                }, 0);
                            }
                        } else {
                            InputStream storedInputStream = jarFile.getInputStream(jarEntry);
                            int count;
                            byte[] buffer = new byte[8192];
                            while((count = storedInputStream.read(buffer)) > 0) {
                                zipOutputStream.write(buffer, 0, count);
                            }
                        }
                        zipOutputStream.flush();
                    } catch(IOException ex){
                        Logger.getLogger(MonkeyPatcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                zipOutputStream.finish();
            }
        } catch(IOException ex){
            // NO-OP
        }

        return jfxAntJar.toURI().toURL();
    }

    private MethodVisitor monkeyPatch_WinAppBundler() {
        String javalangThrowable = "java/lang/Throwable";
        String javaioFile = "java/io/File";
        String javalangString = "java/lang/String";
        // luckily ASM lies inside Gradle ;)
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PRIVATE, METHOD_TO_MONKEY_PATCH, METHOD_SIGNATURE_TO_MONKEY_PATCH, null, new String[]{"java/io/IOException"});

        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, javalangThrowable);
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        mv.visitTryCatchBlock(l3, l4, l5, javalangThrowable);
        Label l6 = new Label();
        mv.visitTryCatchBlock(l3, l4, l6, null);
        Label l7 = new Label();
        Label l8 = new Label();
        Label l9 = new Label();
        mv.visitTryCatchBlock(l7, l8, l9, javalangThrowable);
        Label l10 = new Label();
        mv.visitTryCatchBlock(l5, l10, l6, null);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 3);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        Label l11 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l11);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, javaioFile, "isDirectory", "()Z", false);
        Label l12 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNE, l12);
        mv.visitLabel(l11);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{javalangString}, 0, null);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, javaioFile);
        mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
        mv.visitLdcInsn("java.home");
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, javaioFile, "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 2);
        mv.visitLabel(l12);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.GETSTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "VS_VERS", "[Ljava/lang/String;");
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 4);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 4);
        mv.visitInsn(org.objectweb.asm.Opcodes.ARRAYLENGTH);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ISTORE, 5);
        mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ISTORE, 6);
        Label l13 = new Label();
        mv.visitLabel(l13);
        mv.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"[Ljava/lang/String;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 6);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 5);
        Label l14 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IF_ICMPGE, l14);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 4);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 6);
        mv.visitInsn(org.objectweb.asm.Opcodes.AALOAD);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "com/oracle/tools/packager/windows/WinAppBundler", "copyMSVCDLLs", "(Ljava/io/File;Ljava/lang/String;)Z", false);
        Label l15 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFEQ, l15);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 3);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l14);
        mv.visitLabel(l15);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitIincInsn(6, 1);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l13);
        mv.visitLabel(l14);
        mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 3);
        Label l16 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNONNULL, l16);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
        mv.visitLdcInsn("Not found MSVC dlls");
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
        mv.visitLabel(l16);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/util/concurrent/atomic/AtomicReference");
        mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/util/concurrent/atomic/AtomicReference", "<init>", "()V", false);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 4);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 3);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 5);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, javaioFile, "toPath", "()Ljava/nio/file/Path;", false);
        mv.visitLdcInsn("bin");
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "resolve", "(Ljava/lang/String;)Ljava/nio/file/Path;", true);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/nio/file/Files", "list", "(Ljava/nio/file/Path;)Ljava/util/stream/Stream;", false);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 6);
        mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 7);
        mv.visitLabel(l3);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitInvokeDynamicInsn("test", "()Ljava/util/function/Predicate;", new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)Z"), new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$8", "(Ljava/nio/file/Path;)Z"), Type.getType("(Ljava/nio/file/Path;)Z")});
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "filter", "(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", true);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 5);
        mv.visitInvokeDynamicInsn("test", "(Ljava/lang/String;)Ljava/util/function/Predicate;", new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)Z"), new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$9", "(Ljava/lang/String;Ljava/nio/file/Path;)Z"), Type.getType("(Ljava/nio/file/Path;)Z")});
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "filter", "(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", true);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 4);
        mv.visitInvokeDynamicInsn("accept", "(Ljava/io/File;Ljava/util/concurrent/atomic/AtomicReference;)Ljava/util/function/Consumer;", new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)V"), new Handle(org.objectweb.asm.Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$10", "(Ljava/io/File;Ljava/util/concurrent/atomic/AtomicReference;Ljava/nio/file/Path;)V"), Type.getType("(Ljava/nio/file/Path;)V")});
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "forEach", "(Ljava/util/function/Consumer;)V", true);
        mv.visitLabel(l4);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        Label l17 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l17);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        Label l18 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l18);
        mv.visitLabel(l0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
        mv.visitLabel(l1);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l17);
        mv.visitLabel(l2);
        mv.visitFrame(Opcodes.F_FULL, 8, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString, "java/util/stream/Stream", javalangThrowable}, 1, new Object[]{javalangThrowable});
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 8);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, javalangThrowable, "addSuppressed", "(Ljava/lang/Throwable;)V", false);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l17);
        mv.visitLabel(l18);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l17);
        mv.visitLabel(l5);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{javalangThrowable});
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 8);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
        mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
        mv.visitLabel(l6);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{javalangThrowable});
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 9);
        mv.visitLabel(l10);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        Label l19 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l19);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        Label l20 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l20);
        mv.visitLabel(l7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
        mv.visitLabel(l8);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l19);
        mv.visitLabel(l9);
        mv.visitFrame(Opcodes.F_FULL, 10, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString, "java/util/stream/Stream", javalangThrowable, Opcodes.TOP, javalangThrowable}, 1, new Object[]{javalangThrowable});
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 10);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 10);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, javalangThrowable, "addSuppressed", "(Ljava/lang/Throwable;)V", false);
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l19);
        mv.visitLabel(l20);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
        mv.visitLabel(l19);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 9);
        mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
        mv.visitLabel(l17);
        mv.visitFrame(Opcodes.F_FULL, 6, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString}, 0, new Object[]{});
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 4);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicReference", "get", "()Ljava/lang/Object;", false);
        mv.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "java/io/IOException");
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 6);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        Label l21 = new Label();
        mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l21);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
        mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
        mv.visitLabel(l21);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/io/IOException"}, 0, null);
        mv.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        mv.visitMaxs(3, 11);
        mv.visitEnd();

        return mv;
    }
}
