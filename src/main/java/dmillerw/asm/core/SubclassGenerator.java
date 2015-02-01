package dmillerw.asm.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dmillerw.asm.annotation.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class SubclassGenerator<T> {

    public static <T> Class<T> generateSubclass(Class<?> superClass, Class<? extends Template<T>> templateClass) {
        SubclassGenerator<T> subclassGenerator = new SubclassGenerator<T>(superClass, templateClass);
        return subclassGenerator.generateSubclass();
    }

    private static final ASMClassLoader LOADER = new ASMClassLoader();

    private static class ASMClassLoader extends ClassLoader {

        private ASMClassLoader() {
            super(ASMClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, byte[] data) {
            return defineClass(name, data, 0, data.length);
        }
    }

    private static final boolean DEBUG = true;

    private static void debug(String msg) {
        if (DEBUG)
            System.out.println("DEBUG: " + msg);
    }

    // Class instances
    final Class<?> superClass;
    final Class<? extends Template<?>> templateClass;

    // Template class node. Used for copying methods
    final ClassNode templateNode;

    // Internal class names
    final String superType;
    String subName;
    String subType;
    final String templateType;

    // All methods of the superclass and its superclasses
    final List<MethodNode> superclassMethods = new ArrayList<MethodNode>();

    // Mappings of all valid constructors found in the super class
    final Set<MethodMapping> superConstructors = Sets.newHashSet();
    // Mappings of all valid constructors found in the template
    final Set<MethodMapping> templateConstructors = Sets.newHashSet();

    // All collected method nodes. Can be for constructors, overrides, or implementations
    final Map<MethodMapping, MethodNode> methodNodes = Maps.newHashMap();

    // All methods that the template will override
    final Set<MethodMapping> overrideMethods = Sets.newHashSet();

    // All methods that will be implemented in the sub-class from the template
    final Set<MethodMapping> implementMethods = Sets.newHashSet();

    // Mappings of all fields found in the template
    final Set<FieldMapping> implementFields = Sets.newHashSet();

    // All collected field nodes. Used for copying mainly
    final Map<FieldMapping, FieldNode> fieldNodes = Maps.newHashMap();

    public SubclassGenerator(Class<?> superClass, Class<? extends Template<T>> templateClass) {
        this.superClass = superClass;
        this.templateClass = templateClass;
        this.templateNode = ASMUtils.getClassNode(templateClass);
        this.superType = Type.getInternalName(superClass);
        this.subName = superClass.getName() + "_GENERATED_" + templateClass.hashCode();
        this.subType = subName.replace(".", "/");
        this.templateType = Type.getInternalName(templateClass);

        gatherSuperclassConstructors();
        gatherSuperclassMethods();
        gatherTemplateFields();
        gatherTemplateMethods();
    }

    public SubclassGenerator<T> setClassName(String name) {
        this.subName = name;
        this.subType = subName.replace(".", "/");
        return this;
    }

    /**
     * Gather all constructors directly declared in the super class
     */
    private void gatherSuperclassConstructors() {
        for (Constructor constructor : superClass.getDeclaredConstructors()) {
            MethodMapping methodMapping = new MethodMapping(constructor);
            superConstructors.add(methodMapping);
            debug("Found super-class constructor: " + methodMapping.toString());
        }
    }

    /**
     * Gather all methods in the super class
     */
    private void gatherSuperclassMethods() {
        Class<?> currentClass = superClass;
        while (currentClass != null) {
            ClassNode classNode = ASMUtils.getClassNode(currentClass);
            superclassMethods.addAll(classNode.methods);

            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Gather all annotated fields directly declared in the template class
     */
    private void gatherTemplateFields() {
        for (Field field : templateClass.getDeclaredFields()) {
            if (Modifier.isAbstract(field.getModifiers()))
                continue;

            MField mField = field.getAnnotation(MField.class);

            if (mField != null) {
                FieldMapping fieldMapping = new FieldMapping(field);

                debug("Found annotated field in template: " + fieldMapping.toString());

                implementFields.add(fieldMapping);

                for (FieldNode fieldNode : templateNode.fields) {
                    if (fieldNode.name.equals(fieldMapping.name) && fieldNode.desc.equals(fieldMapping.signature)) {
                        fieldNodes.put(fieldMapping, fieldNode);
                    }
                }
            }
        }
    }

    /**
     * Gather all annotated methods directly declared in the template class, and sanity checking
     */
    private void gatherTemplateMethods() {
        for (Method method : templateClass.getDeclaredMethods()) {
            MConstructor mConstructor = method.getAnnotation(MConstructor.class);
            MOverride mOverride = method.getAnnotation(MOverride.class);
            MImplement mImplement = method.getAnnotation(MImplement.class);
            MCastParam mCastParam = method.getAnnotation(MCastParam.class);
            MCastParamList mCastParamList = method.getAnnotation(MCastParamList.class);

            if (mConstructor != null) {
                MethodMapping methodMapping = new MethodMapping(method);

                methodMapping.signature = methodMapping.signature.substring(0, methodMapping.signature.length() - 1);

                if (mCastParam != null) {
                    methodMapping.signature = ASMUtils.castSignature(methodMapping.signature, mCastParam);
                }

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature + "V")) {
                        methodNodes.put(methodMapping, methodNode);
                    }
                }

                debug("Found template constructor: " + methodMapping.toString());

                templateConstructors.add(methodMapping);
            } else if (mOverride != null) {
                MethodMapping methodMapping = new MethodMapping(method);
                String original = methodMapping.signature;

                debug("Overridding method: " + methodMapping);

                if (mCastParam != null) {
                    String cast = ASMUtils.castSignature(original, mCastParam);
                    debug("Found MCastParam annotation. Changing " + original + " to " + cast);
                    methodMapping.signature = cast;
                }

                if (mCastParamList != null) {
                    String cast = original;
                    for (MCastParam castParam : mCastParamList.castParams()) {
                        cast = ASMUtils.castSignature(cast, castParam);
                    }
                    debug("Found MCastParamList annotation. Changing " + original + " to " + cast);
                    methodMapping.signature = cast;
                }

                boolean foundInSuperClass = false;

                // We're overriding a method. Make sure the superclass actually has it
                for (MethodNode methodNode : superclassMethods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature)) {
                        foundInSuperClass = true;
                        break;
                    }
                }

                if (foundInSuperClass) {
                    overrideMethods.add(methodMapping);

                    for (MethodNode methodNode : templateNode.methods) {
                        // We use original here just in-case the signature was modified, as the template will
                        // still be using the old signature
                        if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(original)) {
                            methodNodes.put(methodMapping, methodNode);
                            break;
                        }
                    }

                    // Also grab the method node from the super class and store
                    MethodMapping defMethodMapping = new MethodMapping("default_" + methodMapping.name, methodMapping.signature);
                    for (MethodNode methodNode : superclassMethods) {
                        if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature)) {
                            methodNodes.put(defMethodMapping, methodNode);
                        }
                    }
                } else {
                    debug("Failed to override " + method);
                }
            } else if (mImplement != null) {
                MethodMapping methodMapping = new MethodMapping(method);
                String original = methodMapping.signature;

                if (mCastParam != null) {
                    String cast = ASMUtils.castSignature(original, mCastParam);
                    debug("Found MCastParam annotation. Changing " + original + " to " + cast);
                    methodMapping.signature = cast;
                }

                if (mCastParamList != null) {
                    String cast = original;
                    for (MCastParam castParam : mCastParamList.castParams()) {
                        cast = ASMUtils.castSignature(cast, castParam);
                    }
                    debug("Found MCastParamList annotation. Changing " + original + " to " + cast);
                    methodMapping.signature = cast;
                }

                implementMethods.add(methodMapping);

                for (MethodNode methodNode : templateNode.methods) {
                    // We use original here just in-case the signature was modified, as the template will
                    // still be using the old signature
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(original)) {
                        methodNodes.put(methodMapping, methodNode);
                    }
                }
            }
        }
    }

    public Class<T> generateSubclass() {
        // All interfaces that the template class implements
        Class<?>[] interfaces = templateClass.getInterfaces();
        // The fully qualified type strings for those interfaces
        String[] interfaceStrs = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceStrs[i] = interfaces[i].getName().replace(".", "/");
        }

        ClassWriter classWriter = new ClassWriter(0);

        // Write class header
        classWriter.visit(
                V1_6,
                ACC_PUBLIC | ACC_SUPER,
                subType,
                null,
                superType,
                interfaceStrs
        );

        classWriter.visitSource(".dynamic", null);

        addFields(classWriter);
        addConstructors(classWriter);
        overrideMethods(classWriter);
        implementMethods(classWriter);

        classWriter.visitEnd();

        Class<?> clazz = LOADER.define(subName, classWriter.toByteArray());
        return (Class<T>) clazz;
    }

    private void addFields(ClassWriter classWriter) {
        for (FieldNode fieldNode : fieldNodes.values()) {
            classWriter.visitField(fieldNode.access, fieldNode.name, fieldNode.desc, null, null);
        }
    }

    private void addConstructors(ClassWriter classWriter) {
        for (MethodMapping methodMapping : superConstructors) {
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", methodMapping.signature + "V", null, null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(ALOAD, 0);
            for (int i = 0; i < methodMapping.params.length; i++) {
                methodVisitor.visitVarInsn(ASMUtils.getLoadCode(methodMapping.params[i]), i + 1);
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL, superType, "<init>", methodMapping.signature + "V", false);

            int maxStack = methodMapping.params.length + 1;
            int maxLocals = methodMapping.params.length + 2;

            // If the template has the same constructor
            // We loop because the constructors found in template are proper methods, and have names
            for (MethodMapping methodMapping1 : templateConstructors) {
                if (methodMapping1.signature.equals(methodMapping.signature)) {
                    debug("Found matching super constructor in template: " + methodMapping1);
                    MethodNode methodNode = methodNodes.get(methodMapping1);

                    InsnList insnList = interpretAndCopyNodes(methodNode);
                    insnList.accept(methodVisitor);

                    maxStack += methodNode.maxStack;
                    maxLocals += methodNode.maxLocals;

                    break;
                }
            }

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(maxStack, maxLocals);
            methodVisitor.visitEnd();
        }
    }

    private void overrideMethods(ClassWriter classWriter) {
        for (MethodMapping methodMapping : overrideMethods) {
            MethodNode methodNode = methodNodes.get(methodMapping);
            MethodMapping defMethodMapping = new MethodMapping("default_" + methodMapping.name, methodMapping.signature);
            MethodNode defNode = methodNodes.get(defMethodMapping);

            MethodVisitor methodVisitor;
            InsnList insnList;

            // Generate a new method that contains the super-class method instructions
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "default_" + defNode.name, defNode.desc, null, null);
            methodVisitor.visitCode();

            insnList = new InsnList();

            Iterator<AbstractInsnNode> iterator = defNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();

                if (insnNode instanceof LabelNode || insnNode instanceof LineNumberNode)
                    continue;

                insnList.add(insnNode);
            }

            insnList.accept(methodVisitor);

            methodVisitor.visitMaxs(defNode.maxStack, defNode.maxLocals);
            methodVisitor.visitEnd();

            // Then generate the override method
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, methodMapping.name, methodMapping.signature, null, null);
            methodVisitor.visitCode();

            insnList = interpretAndCopyNodes(methodNode);

            insnList.accept(methodVisitor);

            methodVisitor.visitMaxs(methodMapping.params.length + 1 + methodNode.maxStack, methodMapping.params.length + 1 + methodNode.maxLocals);

            methodVisitor.visitEnd();
        }
    }

    private void implementMethods(ClassWriter classWriter) {
        for (MethodMapping methodMapping : implementMethods) {
            MethodNode methodNode = methodNodes.get(methodMapping);
            String desc = methodNode.desc;

            MethodVisitor methodVisitor;
            InsnList insnList;

            methodVisitor = classWriter.visitMethod(methodNode.access, methodMapping.name, desc, null, null);
            methodVisitor.visitCode();

            insnList = interpretAndCopyNodes(methodNode);

            insnList.accept(methodVisitor);

            methodVisitor.visitMaxs(methodNode.maxStack, methodNode.maxLocals);
            methodVisitor.visitEnd();
        }
    }

    /**
     * Reads all instructions from a method node, and copies them into a new InsnList
     * <p/>
     * If the node needs to be modified at all (redirects, super calls) that's done as well
     */
    private InsnList interpretAndCopyNodes(MethodNode methodNode) {
        NodeCopier nodeCopier = new NodeCopier(methodNode.instructions);
        InsnList insnList = new InsnList();

        int skip = 0;
        int index = 0;

        Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode insnNode = iterator.next();

            if (skip > 0) {
                debug("Skipping {" + ASMUtils.nodeToString(insnNode) + "} " + skip + " left.");
                skip--;
                index++;
                continue;
            }

            if (insnNode instanceof MethodInsnNode) {
                AbstractInsnNode newNode = redirectLocalMethod((MethodInsnNode) insnNode);
                if (newNode != null) {
                    nodeCopier.copyTo(newNode, insnList);
                } else {
                    nodeCopier.copyTo(insnNode, insnList);
                }
            } else if (insnNode instanceof FieldInsnNode) {
                AbstractInsnNode newNode = redirectSuperCall(methodNode, (FieldInsnNode) insnNode, index);
                if (newNode != null) {
                    debug("Redirected super call!");
                    debug(" * OLD: " + ASMUtils.nodeToString(insnNode));
                    debug(" * NEW: " + ASMUtils.nodeToString(newNode));

                    skip = 2; // Skipping the GETFIELD and the CHECKCAST

                    nodeCopier.copyTo(newNode, insnList);
                } else {
                    newNode = redirectLocalField((FieldInsnNode) insnNode);
                    if (newNode != null) {
                        nodeCopier.copyTo(newNode, insnList);
                    } else {
                        nodeCopier.copyTo(insnNode, insnList);
                    }
                }
            } else {
                nodeCopier.copyTo(insnNode, insnList);
            }

            index++;
        }

        return insnList;
    }

    /**
     * Take a FieldInsnNode and determines whether or not it should be treated as a super call
     * If so, it looks and sees whether it's a field or method call, and delegates accordingly
     * <p/>
     * If it's a method call, we check to see if there's a default_ method, and if so, we redirect
     * the call to the that default method, removing the in-between bytecode (GETFIELD and CHECKCAST)
     * <p/>
     * If it's a field call, we simply chop out the GETFIELD call to the super field, and redirect directly
     * to the field in the subclass
     *
     * @return Whatever node has been generated to properly redirect
     */
    private AbstractInsnNode redirectSuperCall(MethodNode methodNode, FieldInsnNode fieldNode, int index) {
        if (fieldNode.name.equals("_super") && fieldNode.getOpcode() == GETFIELD) {
            AbstractInsnNode nextNode = methodNode.instructions.get(index + 1);

            // We're for certain handling usage of the _super field
            if (nextNode.getOpcode() == CHECKCAST) {
                nextNode = methodNode.instructions.get(index + 2);

                if (nextNode instanceof MethodInsnNode) {
                    MethodInsnNode nextMethodNode = (MethodInsnNode) nextNode;
                    MethodMapping oldMethodMapping = new MethodMapping(nextMethodNode.name, nextMethodNode.desc);

                    // If there's a super call to a method that's been overridden, pass it through
                    // to the generated default method
                    if (overrideMethods.contains(oldMethodMapping)) {
                        debug("Found super call to overridden method!");
                        // Fun fact. This somehow handles super super super methods and I don't even know how
                        return new MethodInsnNode(INVOKESPECIAL, subType, "default_" + nextMethodNode.name, nextMethodNode.desc, false);
                    } else {
                        return new MethodInsnNode(INVOKESPECIAL, superType, nextMethodNode.name, nextMethodNode.desc, false);
                    }
                } else if (nextNode instanceof FieldInsnNode) {
                    FieldInsnNode nextFieldNode = (FieldInsnNode) nextNode;
                    return ASMUtils.redirect(nextFieldNode, subType);
                } else {
                    return null; // Shouldn't happen
                }
            } else {
                return null; // Also shouldn't happen
            }
        } else {
            return null;
        }
    }

    /**
     * Takes a FieldInsnNode and re-directs it to the defined super class IF and ONLY IF it currently points
     * to the template as its owner
     */
    private AbstractInsnNode redirectLocalField(FieldInsnNode fieldNode) {
        if (fieldNode.owner.equals(templateType)) {
            return ASMUtils.redirect(fieldNode, subType);
        } else {
            return null;
        }
    }

    /**
     * Takes a MethodInsnNode and re-directs it to the defined super class IF and ONLY IF it currently points
     * to the template as its owner
     */
    private AbstractInsnNode redirectLocalMethod(MethodInsnNode methodInsnNode) {
        if (methodInsnNode.owner.equals(templateType)) {
            return ASMUtils.redirect(methodInsnNode, subType);
        } else {
            return null;
        }
    }
}
