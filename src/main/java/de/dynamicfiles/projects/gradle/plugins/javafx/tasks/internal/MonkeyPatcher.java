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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.InstructionConst;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;

/**
 *
 * @author Danny Althoff
 */
public class MonkeyPatcher {

    private static final String METHOD_TO_MONKEY_PATCH = "copyMSVCDLLs";
    private static final String METHOD_SIGNATURE_TO_MONKEY_PATCH = "(Ljava/io/File;Ljava/io/File;)V";
    private static final String FAULTY_CLASSFILE_TO_MONKEY_PATCH = "com/oracle/tools/packager/windows/WinAppBundler.class";

    public static URL getPatchedJfxAntJar() throws MalformedURLException {
        String jfxAntJarPath = "/../lib/ant-javafx.jar";
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
            Path tempDirectory = Files.createTempDirectory("javafx-gradle-plugin-workaround");
            // delete that crap after JVM being shut down
//            tempDirectory.toFile().deleteOnExit();

            JarFile jarFile = new JarFile(jfxAntJar, false, JarFile.OPEN_READ);
            File targetManipulatedJarFile = tempDirectory.resolve("ant-javafx.jar").toAbsolutePath().toFile();
            // delete that crap after JVM being shut down
//            targetManipulatedJarFile.deleteOnExit();

            AtomicBoolean useModifiedVersion = new AtomicBoolean(false);

            try(FileOutputStream processedAntJar = new FileOutputStream(targetManipulatedJarFile)){
                ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(processedAntJar);
                jarFile.stream().forEachOrdered(jarEntry -> {
                    System.out.println("copying entry > " + jarEntry.getName());
                    ZipEntry zipEntry = new ZipEntry(jarEntry.getName());
                    try{
                        zipOutputStream.putNextEntry(zipEntry);

                        if( jarEntry.getName().equals(FAULTY_CLASSFILE_TO_MONKEY_PATCH) ){
                            System.out.println("WE FOUND OUR BUGGY CLASS");
                            useModifiedVersion.set(true);

                            ClassParser classParser = new ClassParser(jarFile.getInputStream(jarEntry), FAULTY_CLASSFILE_TO_MONKEY_PATCH);
                            JavaClass parsedJavaClass = classParser.parse();

                            ClassGen classGenerator = new ClassGen(parsedJavaClass);
                            Arrays.asList(parsedJavaClass.getMethods()).stream().forEach(method -> {
                                if( method.getName().equals(METHOD_TO_MONKEY_PATCH) && method.getSignature().equals(METHOD_SIGNATURE_TO_MONKEY_PATCH) ){
                                    classGenerator.removeMethod(method);
                                    System.out.println("found evil beast");
//                                    createMonkeyPatchedMethodFromBCEL(classGenerator.getConstantPool(), new InstructionFactory(classGenerator, classGenerator.getConstantPool()));
//                                    classGenerator.replaceMethod(method, createMonkeyPatchedMethodFromBCEL(classGenerator, method));
                                    classGenerator.addMethod(createMonkeyPatchedMethodFromBCEL(classGenerator, method));
                                }
                            });

                            classGenerator.update();

                            zipOutputStream.write(classGenerator.getJavaClass().getBytes());
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

    private static Attribute getFixedMethodCodeAttribute() {
        AtomicReference<Attribute> foundMethodAttribute = new AtomicReference<>(null);
        try{
            ClassParser classParser = new ClassParser(Thread.currentThread().getContextClassLoader().getResourceAsStream(MonkeyPatcher.class.getName().replace(".", "/").replace("/MonkeyPatcher", "/") + "WinAppBundler_fixed.class"), FAULTY_CLASSFILE_TO_MONKEY_PATCH);
            JavaClass parsedJavaClass = classParser.parse();

            ClassGen classGenerator = new ClassGen(parsedJavaClass);
            Arrays.asList(classGenerator.getMethods()).stream().forEach(method -> {
                if( method.getName().equals(METHOD_TO_MONKEY_PATCH) && method.getSignature().equals(METHOD_SIGNATURE_TO_MONKEY_PATCH) ){
                    MethodGen methodGen = new MethodGen(method, METHOD_TO_MONKEY_PATCH, classGenerator.getConstantPool());
                    Arrays.asList(methodGen.getCodeAttributes()).stream().forEach(attribute -> {
                        if( "StackMapTable".equals(attribute.getName()) ){
                            foundMethodAttribute.set(attribute);
                        }
                    });
                }
            });
        } catch(IOException | ClassFormatException ex){
            Logger.getLogger(MonkeyPatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        return foundMethodAttribute.get();
    }

    private static Method createMonkeyPatchedMethodFromBCEL(ClassGen classGenerator, Method originalMethod) {
        // java -cp "bcel-6.0.jar;." org.apache.bcel.util.BCELifier WinAppBundler_180_92.class >> WinAppBundler_180_92.java
        InstructionList il = new InstructionList();
        ConstantPoolGen constPoolGen = classGenerator.getConstantPool();
//        MethodGen method = new MethodGen(Const.ACC_PRIVATE, Type.VOID, new Type[]{new ObjectType("java.io.File"), new ObjectType("java.io.File")}, new String[]{"arg0", "arg1"}, "copyMSVCDLLs", "com.oracle.tools.packager.windows.WinAppBundler", il, constPoolGen);
        InstructionFactory _factory = new InstructionFactory(classGenerator, constPoolGen);
        MethodGen method = new MethodGen(originalMethod, classGenerator.getFileName(), constPoolGen);
        method.setInstructionList(il);

        InstructionHandle ih_0 = il.append(InstructionConst.ACONST_NULL);
        il.append(_factory.createStore(Type.OBJECT, 3));
        il.append(_factory.createLoad(Type.OBJECT, 2));
        BranchInstruction ifnull_3 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_3);
        il.append(_factory.createLoad(Type.OBJECT, 2));
        il.append(_factory.createInvoke("java.io.File", "isDirectory", Type.BOOLEAN, Type.NO_ARGS, Const.INVOKEVIRTUAL));
        BranchInstruction ifne_10 = _factory.createBranchInstruction(Const.IFNE, null);
        il.append(ifne_10);
        InstructionHandle ih_13 = il.append(_factory.createNew("java.io.File"));
        il.append(InstructionConst.DUP);
        il.append(new PUSH(constPoolGen, "java.home"));
        il.append(_factory.createInvoke("java.lang.System", "getProperty", Type.STRING, new Type[]{Type.STRING}, Const.INVOKESTATIC));
        il.append(_factory.createInvoke("java.io.File", "<init>", Type.VOID, new Type[]{Type.STRING}, Const.INVOKESPECIAL));
        il.append(_factory.createStore(Type.OBJECT, 2));
        InstructionHandle ih_26 = il.append(_factory.createFieldAccess("com.oracle.tools.packager.windows.WinAppBundler", "VS_VERS", new ArrayType(Type.STRING, 1), Const.GETSTATIC));
        il.append(_factory.createStore(Type.OBJECT, 4));
        il.append(_factory.createLoad(Type.OBJECT, 4));
        il.append(InstructionConst.ARRAYLENGTH);
        il.append(_factory.createStore(Type.INT, 5));
        il.append(new PUSH(constPoolGen, 0));
        il.append(_factory.createStore(Type.INT, 6));
        InstructionHandle ih_39 = il.append(_factory.createLoad(Type.INT, 6));
        il.append(_factory.createLoad(Type.INT, 5));
        BranchInstruction if_icmpge_43 = _factory.createBranchInstruction(Const.IF_ICMPGE, null);
        il.append(if_icmpge_43);
        il.append(_factory.createLoad(Type.OBJECT, 4));
        il.append(_factory.createLoad(Type.INT, 6));
        il.append(InstructionConst.AALOAD);
        il.append(_factory.createStore(Type.OBJECT, 7));
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createLoad(Type.OBJECT, 7));
        il.append(_factory.createInvoke("com.oracle.tools.packager.windows.WinAppBundler", "copyMSVCDLLs", Type.BOOLEAN, new Type[]{new ObjectType("java.io.File"), Type.STRING}, Const.INVOKESPECIAL));
        BranchInstruction ifeq_60 = _factory.createBranchInstruction(Const.IFEQ, null);
        il.append(ifeq_60);
        il.append(_factory.createLoad(Type.OBJECT, 7));
        il.append(_factory.createStore(Type.OBJECT, 3));
        BranchInstruction goto_66 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_66);
        InstructionHandle ih_69 = il.append(new IINC(6, 1));
        BranchInstruction goto_72 = _factory.createBranchInstruction(Const.GOTO, ih_39);
        il.append(goto_72);
        InstructionHandle ih_75 = il.append(_factory.createLoad(Type.OBJECT, 3));
        BranchInstruction ifnonnull_76 = _factory.createBranchInstruction(Const.IFNONNULL, null);
        il.append(ifnonnull_76);
        il.append(_factory.createNew("java.lang.RuntimeException"));
        il.append(InstructionConst.DUP);
        il.append(new PUSH(constPoolGen, "Not found MSVC dlls"));
        il.append(_factory.createInvoke("java.lang.RuntimeException", "<init>", Type.VOID, new Type[]{Type.STRING}, Const.INVOKESPECIAL));
        il.append(InstructionConst.ATHROW);
        InstructionHandle ih_89 = il.append(_factory.createNew("java.util.concurrent.atomic.AtomicReference"));
        il.append(InstructionConst.DUP);
        il.append(_factory.createInvoke("java.util.concurrent.atomic.AtomicReference", "<init>", Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
        il.append(_factory.createStore(Type.OBJECT, 4));
        il.append(_factory.createLoad(Type.OBJECT, 3));
        il.append(_factory.createStore(Type.OBJECT, 5));
        il.append(_factory.createLoad(Type.OBJECT, 2));
        il.append(_factory.createInvoke("java.io.File", "toPath", new ObjectType("java.nio.file.Path"), Type.NO_ARGS, Const.INVOKEVIRTUAL));
        il.append(new PUSH(constPoolGen, "bin"));
        il.append(_factory.createInvoke("java.nio.file.Path", "resolve", new ObjectType("java.nio.file.Path"), new Type[]{Type.STRING}, Const.INVOKEINTERFACE));
        il.append(_factory.createInvoke("java.nio.file.Files", "list", new ObjectType("java.util.stream.Stream"), new Type[]{new ObjectType("java.nio.file.Path")}, Const.INVOKESTATIC));
        il.append(_factory.createStore(Type.OBJECT, 6));
        il.append(InstructionConst.ACONST_NULL);
        il.append(_factory.createStore(Type.OBJECT, 7));
        InstructionHandle ih_120 = il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke("test", "test", new ObjectType("java.util.function.Predicate"), Type.NO_ARGS, Const.INVOKEDYNAMIC));
        il.append(_factory.createInvoke("java.util.stream.Stream", "filter", new ObjectType("java.util.stream.Stream"), new Type[]{new ObjectType("java.util.function.Predicate")}, Const.INVOKEINTERFACE));
        il.append(_factory.createLoad(Type.OBJECT, 5));
        il.append(_factory.createInvoke("test", "test", new ObjectType("java.util.function.Predicate"), new Type[]{Type.STRING}, Const.INVOKEDYNAMIC));
        il.append(_factory.createInvoke("java.util.stream.Stream", "filter", new ObjectType("java.util.stream.Stream"), new Type[]{new ObjectType("java.util.function.Predicate")}, Const.INVOKEINTERFACE));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createLoad(Type.OBJECT, 4));
        il.append(_factory.createInvoke("accept", "accept", new ObjectType("java.util.function.Consumer"), new Type[]{new ObjectType("java.io.File"), new ObjectType("java.util.concurrent.atomic.AtomicReference")}, Const.INVOKEDYNAMIC));
        InstructionHandle ih_152 = il.append(_factory.createInvoke("java.util.stream.Stream", "forEach", Type.VOID, new Type[]{new ObjectType("java.util.function.Consumer")}, Const.INVOKEINTERFACE));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        BranchInstruction ifnull_159 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_159);
        il.append(_factory.createLoad(Type.OBJECT, 7));
        BranchInstruction ifnull_164 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_164);
        InstructionHandle ih_167 = il.append(_factory.createLoad(Type.OBJECT, 6));
        InstructionHandle ih_169 = il.append(_factory.createInvoke("java.util.stream.Stream", "close", Type.VOID, Type.NO_ARGS, Const.INVOKEINTERFACE));
        BranchInstruction goto_174 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_174);
        InstructionHandle ih_177 = il.append(_factory.createStore(Type.OBJECT, 8));
        il.append(_factory.createLoad(Type.OBJECT, 7));
        il.append(_factory.createLoad(Type.OBJECT, 8));
        il.append(_factory.createInvoke("java.lang.Throwable", "addSuppressed", Type.VOID, new Type[]{new ObjectType("java.lang.Throwable")}, Const.INVOKEVIRTUAL));
        BranchInstruction goto_186 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_186);
        InstructionHandle ih_189 = il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke("java.util.stream.Stream", "close", Type.VOID, Type.NO_ARGS, Const.INVOKEINTERFACE));
        BranchInstruction goto_196 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_196);
        InstructionHandle ih_199 = il.append(_factory.createStore(Type.OBJECT, 8));
        il.append(_factory.createLoad(Type.OBJECT, 8));
        il.append(_factory.createStore(Type.OBJECT, 7));
        il.append(_factory.createLoad(Type.OBJECT, 8));
        il.append(InstructionConst.ATHROW);
        InstructionHandle ih_208 = il.append(_factory.createStore(Type.OBJECT, 9));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        BranchInstruction ifnull_212 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_212);
        il.append(_factory.createLoad(Type.OBJECT, 7));
        BranchInstruction ifnull_217 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_217);
        InstructionHandle ih_220 = il.append(_factory.createLoad(Type.OBJECT, 6));
        InstructionHandle ih_222 = il.append(_factory.createInvoke("java.util.stream.Stream", "close", Type.VOID, Type.NO_ARGS, Const.INVOKEINTERFACE));
        BranchInstruction goto_227 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_227);
        InstructionHandle ih_230 = il.append(_factory.createStore(Type.OBJECT, 10));
        il.append(_factory.createLoad(Type.OBJECT, 7));
        il.append(_factory.createLoad(Type.OBJECT, 10));
        il.append(_factory.createInvoke("java.lang.Throwable", "addSuppressed", Type.VOID, new Type[]{new ObjectType("java.lang.Throwable")}, Const.INVOKEVIRTUAL));
        BranchInstruction goto_239 = _factory.createBranchInstruction(Const.GOTO, null);
        il.append(goto_239);
        InstructionHandle ih_242 = il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke("java.util.stream.Stream", "close", Type.VOID, Type.NO_ARGS, Const.INVOKEINTERFACE));
        InstructionHandle ih_249 = il.append(_factory.createLoad(Type.OBJECT, 9));
        il.append(InstructionConst.ATHROW);
        InstructionHandle ih_252 = il.append(_factory.createLoad(Type.OBJECT, 4));
        il.append(_factory.createInvoke("java.util.concurrent.atomic.AtomicReference", "get", Type.OBJECT, Type.NO_ARGS, Const.INVOKEVIRTUAL));
        il.append(_factory.createCheckCast(new ObjectType("java.io.IOException")));
        il.append(_factory.createStore(Type.OBJECT, 6));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        BranchInstruction ifnull_264 = _factory.createBranchInstruction(Const.IFNULL, null);
        il.append(ifnull_264);
        il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(InstructionConst.ATHROW);
        InstructionHandle ih_270 = il.append(_factory.createReturn(Type.VOID));
        ifnull_3.setTarget(ih_13);
        ifne_10.setTarget(ih_26);
        if_icmpge_43.setTarget(ih_75);
        ifeq_60.setTarget(ih_69);
        goto_66.setTarget(ih_75);
        ifnonnull_76.setTarget(ih_89);
        ifnull_159.setTarget(ih_252);
        ifnull_164.setTarget(ih_189);
        goto_174.setTarget(ih_252);
        goto_186.setTarget(ih_252);
        goto_196.setTarget(ih_252);
        ifnull_212.setTarget(ih_249);
        ifnull_217.setTarget(ih_242);
        goto_227.setTarget(ih_249);
        goto_239.setTarget(ih_249);
        ifnull_264.setTarget(ih_270);
        method.addExceptionHandler(ih_167, ih_169, ih_177, new ObjectType("java.lang.Throwable"));
        method.addExceptionHandler(ih_120, ih_152, ih_199, new ObjectType("java.lang.Throwable"));
        method.addExceptionHandler(ih_120, ih_152, ih_208, null);
        method.addExceptionHandler(ih_220, ih_222, ih_230, new ObjectType("java.lang.Throwable"));
        method.addExceptionHandler(ih_199, ih_208, ih_208, null);

        System.out.println("Trying to get fixed stackmap");
        StackMap fixedMethodCodeAttribute = (StackMap) getFixedMethodCodeAttribute();

//        System.out.println("= original stackmap constant pool =");
//        System.out.println(fixedMethodCodeAttribute.getConstantPool().toString());
        System.out.println("Trying to get copy of fixed stackmap with different constant pool");
        StackMap stackmapTable = (StackMap) fixedMethodCodeAttribute.copy(constPoolGen.getConstantPool());
        if( stackmapTable != null ){
            Arrays.asList(stackmapTable.getStackMap()).stream().forEach(stackMapEntry -> {
                // correct entries
                stackMapEntry.setConstantPool(constPoolGen.getConstantPool());
            });
//            System.out.println("= modified stackmap constant pool =");
//            System.out.println(stackmapTable.getConstantPool().toString());
            System.out.println("replacing stackmaptable with fixed one");
            method.removeCodeAttributes();

            System.out.println("NameIndex after cloning > " + stackmapTable.getNameIndex());
            stackmapTable.setConstantPool(constPoolGen.getConstantPool());
            System.out.println("NameIndex after ConstantPool switch > " + stackmapTable.getNameIndex());
            System.out.println("found place of stackmaptable > " + constPoolGen.lookupUtf8("StackMapTable"));
            stackmapTable.setNameIndex(constPoolGen.lookupUtf8("StackMapTable"));
            System.out.println("NameIndex after setting index manually > " + stackmapTable.getNameIndex());

            System.out.println("adding stackmaptable");

            method.addCodeAttribute(stackmapTable);
        } else {
            System.out.println("wasn't able to get fixed stackmaptable");
        }

        System.out.println("removing non-required stuff");
        method.removeExceptionHandlers();
        method.removeNOPs();
        method.removeLineNumbers();

//        method.setMaxStack(7);
//        method.setMaxLocals(11);
        method.update();

        method.setMaxStack();
        method.setMaxLocals();

        method.update();

        System.out.println("generate method");
        return method.getMethod();
    }
}
