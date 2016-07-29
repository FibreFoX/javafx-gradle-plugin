package de.dynamicfiles.projects.gradle.plugins.javafx.tasks.internal;

import de.dynamicfiles.projects.gradle.plugins.javafx.JavaFXGradlePlugin;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.objectweb.asm.*;

/**
 *
 * @author Danny Althoff
 */
public class MonkeyPatcher {

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

        try{
            // TODO patch jar-file
            Path tempDirectory = Files.createTempDirectory("javafx-gradle-plugin-workaround");
            Path copiedAntJar = Files.copy(jfxAntJar.toPath(), tempDirectory.resolve(JavaFXGradlePlugin.ANT_JAVAFX_JAR_FILENAME));
        } catch(IOException ex){
            // NO-OP
        }

        return jfxAntJar.toURI().toURL();
    }

    private void monkeyPatch_WinAppBundler() {
        // luckily ASM lies inside Gradle ;)
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        {
            mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PRIVATE, "copyMSVCDLLs", "(Ljava/io/File;Ljava/io/File;)V", null, new String[]{"java/io/IOException"});
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
            Label l3 = new Label();
            Label l4 = new Label();
            Label l5 = new Label();
            mv.visitTryCatchBlock(l3, l4, l5, "java/lang/Throwable");
            Label l6 = new Label();
            mv.visitTryCatchBlock(l3, l4, l6, null);
            Label l7 = new Label();
            Label l8 = new Label();
            Label l9 = new Label();
            mv.visitTryCatchBlock(l7, l8, l9, "java/lang/Throwable");
            Label l10 = new Label();
            mv.visitTryCatchBlock(l5, l10, l6, null);
            mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 3);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
            Label l11 = new Label();
            mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, l11);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/io/File", "isDirectory", "()Z", false);
            Label l12 = new Label();
            mv.visitJumpInsn(org.objectweb.asm.Opcodes.IFNE, l12);
            mv.visitLabel(l11);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);
            mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/io/File");
            mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
            mv.visitLdcInsn("java.home");
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
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
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/io/File", "toPath", "()Ljava/nio/file/Path;", false);
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
            mv.visitFrame(Opcodes.F_FULL, 8, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", "java/io/File", "java/io/File", "java/lang/String", "java/util/concurrent/atomic/AtomicReference", "java/lang/String", "java/util/stream/Stream", "java/lang/Throwable"}, 1, new Object[]{"java/lang/Throwable"});
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 8);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
            mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l17);
            mv.visitLabel(l18);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 6);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "close", "()V", true);
            mv.visitJumpInsn(org.objectweb.asm.Opcodes.GOTO, l17);
            mv.visitLabel(l5);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 8);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 7);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 8);
            mv.visitInsn(org.objectweb.asm.Opcodes.ATHROW);
            mv.visitLabel(l6);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
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
            mv.visitFrame(Opcodes.F_FULL, 10, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", "java/io/File", "java/io/File", "java/lang/String", "java/util/concurrent/atomic/AtomicReference", "java/lang/String", "java/util/stream/Stream", "java/lang/Throwable", Opcodes.TOP, "java/lang/Throwable"}, 1, new Object[]{"java/lang/Throwable"});
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, 10);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 7);
            mv.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 10);
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
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
            mv.visitFrame(Opcodes.F_FULL, 6, new Object[]{"com/oracle/tools/packager/windows/WinAppBundler", "java/io/File", "java/io/File", "java/lang/String", "java/util/concurrent/atomic/AtomicReference", "java/lang/String"}, 0, new Object[]{});
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
        }
    }
}
