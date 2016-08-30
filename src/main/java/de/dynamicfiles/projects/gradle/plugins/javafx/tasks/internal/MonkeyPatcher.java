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

import static de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePlugin.ANT_JAVAFX_JAR_FILENAME;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.Label;

/**
 *
 * @author Danny Althoff
 */
public class MonkeyPatcher {

    private static final String METHOD_TO_MONKEY_PATCH = "copyMSVCDLLs";
    private static final String METHOD_SIGNATURE_TO_MONKEY_PATCH = "(Ljava/io/File;Ljava/io/File;)V";
    private static final String FAULTY_CLASSFILE_TO_MONKEY_PATCH = "com/oracle/tools/packager/windows/WinAppBundler.class";
    public static final String WORKAROUND_DIRECTORY_NAME = "javafx-gradle-plugin-workaround";

    public static URL getPatchedJfxAntJar() throws MalformedURLException {
        String jfxAntJarPath = "/../lib/" + ANT_JAVAFX_JAR_FILENAME;

        // on java 9, we have a different path
        if( JavaDetectionTools.IS_JAVA_9 ){
            jfxAntJarPath = "/lib/" + ANT_JAVAFX_JAR_FILENAME;
        }

        File jfxAntJar = new File(System.getProperty("java.home") + jfxAntJarPath);

        if( !jfxAntJar.exists() ){
            throw new RuntimeException("Couldn't find Ant-JavaFX-library, please make sure you've installed some JDK which includes JavaFX (e.g. OracleJDK or OpenJDK and OpenJFX), and JAVA_HOME is set properly.");
        }

        // open jar-file as inputstream
        // copy into new jar-file into temp-folder
        // when found special WinAppBundler-file, process this file via ASM
        // generate new class-file
        // write that generated class-file instead of original file to new jar-file
        // return path to that modified jar-file
        try{
            Path tempDirectory = Files.createTempDirectory(WORKAROUND_DIRECTORY_NAME);
            // delete that crap after JVM being shut down
            tempDirectory.toFile().deleteOnExit();

            JarFile jarFile = new JarFile(jfxAntJar, false, JarFile.OPEN_READ);
            File targetManipulatedJarFile = tempDirectory.resolve(ANT_JAVAFX_JAR_FILENAME).toAbsolutePath().toFile();
            // delete that crap after JVM being shut down
            targetManipulatedJarFile.deleteOnExit();

            AtomicBoolean useModifiedVersion = new AtomicBoolean(false);

            try(FileOutputStream processedAntJar = new FileOutputStream(targetManipulatedJarFile)){
                ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(processedAntJar);
                jarFile.stream().forEachOrdered(jarEntry -> {
                    ZipEntry zipEntry = new ZipEntry(jarEntry.getName());
                    try{
                        zipOutputStream.putNextEntry(zipEntry);

                        if( jarEntry.getName().equals(FAULTY_CLASSFILE_TO_MONKEY_PATCH) ){
                            useModifiedVersion.set(true);

                            ClassReader classReader = new ClassReader(jarFile.getInputStream(jarEntry));
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

                            doMonkeyPatchFileHandleLeak(classReader, classWriter);

                            byte[] generatedBytes = classWriter.toByteArray();

                            zipOutputStream.write(generatedBytes);
                        } else {
                            InputStream storedInputStream = jarFile.getInputStream(jarEntry);
                            int count;
                            byte[] buffer = new byte[8192];
                            while((count = storedInputStream.read(buffer)) > 0) {
                                zipOutputStream.write(buffer, 0, count);
                            }
                        }
                        zipOutputStream.flush();
                    } catch(NullPointerException | IOException ex){
                        Logger.getLogger(MonkeyPatcher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
                zipOutputStream.finish();
            }
            if( useModifiedVersion.get() ){
                return targetManipulatedJarFile.toURI().toURL();
            }
        } catch(IOException ex){
            // NO-OP
        }
        return jfxAntJar.toURI().toURL();
    }

    private static void doMonkeyPatchFileHandleLeak(ClassReader classReader, ClassWriter classWriter) {
        classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if( !(name.equals(METHOD_TO_MONKEY_PATCH) && desc.equals(METHOD_SIGNATURE_TO_MONKEY_PATCH)) ){
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
                // helpful source: http://web.cs.ucla.edu/~msb/cs239-tutorial/
                // "We will do this using the Adapter Pattern. Adapters wrap an object, overriding some of its methods, and delegating to the others."
                // ugly adapter-pattern ... took me more time than I really can tell here <.<
                return getMonkeyPatchedFileHandleLeakMethodVisitor(access, name, desc, signature, exceptions);
            }

            private MethodVisitor getMonkeyPatchedFileHandleLeakMethodVisitor(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {

                    /*
                     TODO improve detection of lambda-positions, numbers might vary on different compile-versions
                     */
                    @Override
                    public void visitCode() {
                        // This mostly got generated from ASM itself, except some adjustments for lambda-IDs and removed "visitMaxs()"-call
                        String javalangThrowable = "java/lang/Throwable";
                        String javaioFile = "java/io/File";
                        String javalangString = "java/lang/String";

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
                        mv.visitInsn(Opcodes.ACONST_NULL);
                        mv.visitVarInsn(Opcodes.ASTORE, 3);
                        mv.visitVarInsn(Opcodes.ALOAD, 2);
                        Label l11 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l11);
                        mv.visitVarInsn(Opcodes.ALOAD, 2);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, javaioFile, "isDirectory", "()Z", false);
                        Label l12 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNE, l12);
                        mv.visitLabel(l11);
                        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{javalangString}, 0, null);
                        mv.visitTypeInsn(Opcodes.NEW, javaioFile);
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitLdcInsn("java.home");
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, javaioFile, "<init>", "(Ljava/lang/String;)V", false);
                        mv.visitVarInsn(Opcodes.ASTORE, 2);
                        mv.visitLabel(l12);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "VS_VERS", "[Ljava/lang/String;");
                        mv.visitVarInsn(Opcodes.ASTORE, 4);
                        mv.visitVarInsn(Opcodes.ALOAD, 4);
                        mv.visitInsn(Opcodes.ARRAYLENGTH);
                        mv.visitVarInsn(Opcodes.ISTORE, 5);
                        mv.visitInsn(Opcodes.ICONST_0);
                        mv.visitVarInsn(Opcodes.ISTORE, 6);
                        Label l13 = new Label();
                        mv.visitLabel(l13);
                        mv.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"[Ljava/lang/String;", Opcodes.INTEGER, Opcodes.INTEGER}, 0, null);
                        mv.visitVarInsn(Opcodes.ILOAD, 6);
                        mv.visitVarInsn(Opcodes.ILOAD, 5);
                        Label l14 = new Label();
                        mv.visitJumpInsn(Opcodes.IF_ICMPGE, l14);
                        mv.visitVarInsn(Opcodes.ALOAD, 4);
                        mv.visitVarInsn(Opcodes.ILOAD, 6);
                        mv.visitInsn(Opcodes.AALOAD);
                        mv.visitVarInsn(Opcodes.ASTORE, 7);
                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/oracle/tools/packager/windows/WinAppBundler", "copyMSVCDLLs", "(Ljava/io/File;Ljava/lang/String;)Z", false);
                        Label l15 = new Label();
                        mv.visitJumpInsn(Opcodes.IFEQ, l15);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        mv.visitVarInsn(Opcodes.ASTORE, 3);
                        mv.visitJumpInsn(Opcodes.GOTO, l14);
                        mv.visitLabel(l15);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitIincInsn(6, 1);
                        mv.visitJumpInsn(Opcodes.GOTO, l13);
                        mv.visitLabel(l14);
                        mv.visitFrame(Opcodes.F_CHOP, 3, null, 0, null);
                        mv.visitVarInsn(Opcodes.ALOAD, 3);
                        Label l16 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNONNULL, l16);
                        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitLdcInsn("Not found MSVC dlls");
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
                        mv.visitInsn(Opcodes.ATHROW);
                        mv.visitLabel(l16);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitTypeInsn(Opcodes.NEW, "java/util/concurrent/atomic/AtomicReference");
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/concurrent/atomic/AtomicReference", "<init>", "()V", false);
                        mv.visitVarInsn(Opcodes.ASTORE, 4);
                        mv.visitVarInsn(Opcodes.ALOAD, 3);
                        mv.visitVarInsn(Opcodes.ASTORE, 5);
                        mv.visitVarInsn(Opcodes.ALOAD, 2);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, javaioFile, "toPath", "()Ljava/nio/file/Path;", false);
                        mv.visitLdcInsn("bin");
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "resolve", "(Ljava/lang/String;)Ljava/nio/file/Path;", true);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/file/Files", "list", "(Ljava/nio/file/Path;)Ljava/util/stream/Stream;", false);
                        mv.visitVarInsn(Opcodes.ASTORE, 6);
                        mv.visitInsn(Opcodes.ACONST_NULL);
                        mv.visitVarInsn(Opcodes.ASTORE, 7);
                        mv.visitLabel(l3);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitInvokeDynamicInsn("test", "()Ljava/util/function/Predicate;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)Z"), new Handle(Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$261", "(Ljava/nio/file/Path;)Z"), Type.getType("(Ljava/nio/file/Path;)Z")}); // modified lambda-name
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "filter", "(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", true);
                        mv.visitVarInsn(Opcodes.ALOAD, 5);
                        mv.visitInvokeDynamicInsn("test", "(Ljava/lang/String;)Ljava/util/function/Predicate;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)Z"), new Handle(Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$262", "(Ljava/lang/String;Ljava/nio/file/Path;)Z"), Type.getType("(Ljava/nio/file/Path;)Z")}); // modified lambda-name
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "filter", "(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;", true);
                        mv.visitVarInsn(Opcodes.ALOAD, 1);
                        mv.visitVarInsn(Opcodes.ALOAD, 4);
                        mv.visitInvokeDynamicInsn("accept", "(Ljava/io/File;Ljava/util/concurrent/atomic/AtomicReference;)Ljava/util/function/Consumer;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"), new Object[]{Type.getType("(Ljava/lang/Object;)V"), new Handle(Opcodes.H_INVOKESTATIC, "com/oracle/tools/packager/windows/WinAppBundler", "lambda$copyMSVCDLLs$263", "(Ljava/io/File;Ljava/util/concurrent/atomic/AtomicReference;Ljava/nio/file/Path;)V"), Type.getType("(Ljava/nio/file/Path;)V")}); // modified lambda-name
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "forEach", "(Ljava/util/function/Consumer;)V", true);
                        mv.visitLabel(l4);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        Label l17 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l17);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        Label l18 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l18);
                        mv.visitLabel(l0);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
                        mv.visitLabel(l1);
                        mv.visitJumpInsn(Opcodes.GOTO, l17);
                        mv.visitLabel(l2);
                        mv.visitFrame(Opcodes.F_FULL, 8, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString, "java/util/stream/Stream", javalangThrowable}, 1, new Object[]{javalangThrowable});
                        mv.visitVarInsn(Opcodes.ASTORE, 8);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        mv.visitVarInsn(Opcodes.ALOAD, 8);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, javalangThrowable, "addSuppressed", "(Ljava/lang/Throwable;)V", false);
                        mv.visitJumpInsn(Opcodes.GOTO, l17);
                        mv.visitLabel(l18);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
                        mv.visitJumpInsn(Opcodes.GOTO, l17);
                        mv.visitLabel(l5);
                        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{javalangThrowable});
                        mv.visitVarInsn(Opcodes.ASTORE, 8);
                        mv.visitVarInsn(Opcodes.ALOAD, 8);
                        mv.visitVarInsn(Opcodes.ASTORE, 7);
                        mv.visitVarInsn(Opcodes.ALOAD, 8);
                        mv.visitInsn(Opcodes.ATHROW);
                        mv.visitLabel(l6);
                        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{javalangThrowable});
                        mv.visitVarInsn(Opcodes.ASTORE, 9);
                        mv.visitLabel(l10);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        Label l19 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l19);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        Label l20 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l20);
                        mv.visitLabel(l7);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
                        mv.visitLabel(l8);
                        mv.visitJumpInsn(Opcodes.GOTO, l19);
                        mv.visitLabel(l9);
                        mv.visitFrame(Opcodes.F_FULL, 10, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString, "java/util/stream/Stream", javalangThrowable, Opcodes.TOP, javalangThrowable}, 1, new Object[]{javalangThrowable});
                        mv.visitVarInsn(Opcodes.ASTORE, 10);
                        mv.visitVarInsn(Opcodes.ALOAD, 7);
                        mv.visitVarInsn(Opcodes.ALOAD, 10);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, javalangThrowable, "addSuppressed", "(Ljava/lang/Throwable;)V", false);
                        mv.visitJumpInsn(Opcodes.GOTO, l19);
                        mv.visitLabel(l20);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
                        mv.visitLabel(l19);
                        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        mv.visitVarInsn(Opcodes.ALOAD, 9);
                        mv.visitInsn(Opcodes.ATHROW);
                        mv.visitLabel(l17);
                        mv.visitFrame(Opcodes.F_FULL, 6, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", javaioFile, javaioFile, javalangString, "java/util/concurrent/atomic/AtomicReference", javalangString}, 0, new Object[]{});
                        mv.visitVarInsn(Opcodes.ALOAD, 4);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicReference", "get", "()Ljava/lang/Object;", false);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/io/IOException");
                        mv.visitVarInsn(Opcodes.ASTORE, 6);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        Label l21 = new Label();
                        mv.visitJumpInsn(Opcodes.IFNULL, l21);
                        mv.visitVarInsn(Opcodes.ALOAD, 6);
                        mv.visitInsn(Opcodes.ATHROW);
                        mv.visitLabel(l21);
                        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/io/IOException"}, 0, null);
                        mv.visitInsn(Opcodes.RETURN);
                    }

                };
            }

        }, ClassReader.EXPAND_FRAMES); // ClassReader.EXPAND_FRAMES required for Java 8
    }

}
