package dmillerw.asm.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MField;
import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import jdk.internal.org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class SubclassGenerator<T> {

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

    // Super class node. Used for protecting the original methods when overridden
    final ClassNode superNode;
    // Template class node. Used for copying methods
    final ClassNode templateNode;

    // Internal class names
    final String superType;
    String subName;
    String subType;
    final String templateType;

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

    public SubclassGenerator(Class<T> superClass, Class<? extends Template<?>> templateClass) {
        this.superClass = superClass;
        this.templateClass = templateClass;
        this.superNode = ASMUtils.getClassNode(superClass);
        this.templateNode = ASMUtils.getClassNode(templateClass);
        this.superType = Type.getInternalName(superClass);
        this.subName = superClass.getName() + "_GENERATED_" + templateClass.hashCode();
        this.subType = subName.replace(".", "/");
        this.templateType = Type.getInternalName(templateClass);

        gatherSuperclassConstructors();
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

            if (mConstructor != null) {
                MethodMapping methodMapping = new MethodMapping(method);

                methodMapping.signature = methodMapping.signature.substring(0, methodMapping.signature.length() - 1);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature + "V")) {
                        methodNodes.put(methodMapping, methodNode);
                    }
                }

                debug("Found template constructor: " + methodMapping.toString());

                templateConstructors.add(methodMapping);
            }

            if (mOverride != null) {
                MethodMapping methodMapping = new MethodMapping(method);

                debug("Overridding method: " + methodMapping);

                // We're overriding a method. Make sure the superclass actually has it
                try {
                    superClass.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("Cannot override " + method.getName() + " from " + superType + " as it doesn't exist");
                }

                overrideMethods.add(methodMapping);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature)) {
                        methodNodes.put(methodMapping, methodNode);
                    }
                }

                // Also grab the method node from the super class and store
                MethodMapping defMethodMapping = new MethodMapping("default_" + methodMapping.name, methodMapping.signature);
                for (MethodNode methodNode : superNode.methods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature)) {
                        methodNodes.put(defMethodMapping, methodNode);
                    }
                }
            }

            if (mImplement != null) {
                MethodMapping methodMapping = new MethodMapping(method);

                implementMethods.add(methodMapping);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(methodMapping.name) && methodNode.desc.equals(methodMapping.signature)) {
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

        byte[] data = classWriter.toByteArray();

        /*ClassNode cnode = new ClassNode();
        ClassReader reader = new ClassReader(data);
        reader.accept(cnode, 0);

        for (MethodNode methodNode : cnode.methods) {
            System.out.println(" * * " + methodNode.name + " * * ");
            System.out.println(ASMUtils.insnListToString(methodNode.instructions));
        }*/

        Class<?> clazz = LOADER.define(subName, data);
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

            int maxLocals = methodMapping.params.length + 2;
            int maxStack = methodMapping.params.length + 1;

            InsnList insnList = new InsnList();

            // If the template has the same constructor
            // We loop because the constructors found in template are proper methods, and have names
            for (MethodMapping methodMapping1 : templateConstructors) {
                if (methodMapping1.signature.equals(methodMapping.signature)) {
                    debug("Found matching super constructor in template: " + methodMapping1);

                    MethodNode methodNode = methodNodes.get(methodMapping1);

                    NodeCopier nodeCopier = new NodeCopier(methodNode.instructions);

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
                            AbstractInsnNode newNode = redirectLocalMethod(methodNode, (MethodInsnNode) insnNode, index);
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
                                newNode = redirectLocalField(methodNode, (FieldInsnNode) insnNode, index);
                                if (newNode != null) {
                                    nodeCopier.copyTo(newNode, insnList);
                                } else {
                                    nodeCopier.copyTo(insnNode, insnList);
                                }
                            }
                        } else {
                            // Stop once we get to the return
                            if (insnNode.getOpcode() == RETURN) {
                                maxLocals += methodNode.maxLocals;
                                maxStack += methodNode.maxStack;
                                break;
                            } else {
                                nodeCopier.copyTo(insnNode, insnList);
                            }
                        }

                        index++;
                    }

                    break;
                }
            }

            System.out.println(ASMUtils.insnListToString(insnList));

            insnList.accept(methodVisitor);

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(maxLocals, maxStack);
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

            insnList = new InsnList();

            NodeCopier nodeCopier = new NodeCopier(methodNode.instructions);

            int skip = 0;
            int index = 0;
            iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();

                if (skip > 0) {
                    debug("Skipping {" + ASMUtils.nodeToString(insnNode) + "} " + skip + " left.");
                    skip--;
                    index++;
                    continue;
                }

                if (insnNode instanceof FieldInsnNode) {
                    AbstractInsnNode newNode = redirectSuperCall(methodNode, (FieldInsnNode) insnNode, index);
                    if (newNode != null) {
                        debug("Redirected super call!");
                        debug(" * OLD: " + ASMUtils.nodeToString(insnNode));
                        debug(" * NEW: " + ASMUtils.nodeToString(newNode));

                        skip = 2; // Skipping the GETFIELD and the CHECKCAST

                        nodeCopier.copyTo(newNode, insnList);
                    } else {
                        newNode = redirectLocalField(methodNode, (FieldInsnNode) insnNode, index);
                        if (newNode != null) {
                            nodeCopier.copyTo(newNode, insnList);
                        } else {
                            nodeCopier.copyTo(insnNode, insnList);
                        }
                    }
                } else {
                    /*// Stop once we get to the return
                    if (insnNode.getOpcode() == RETURN) {
                        maxLocals += methodNode.maxLocals;
                        maxStack += methodNode.maxStack;
                        break;
                    } else {*/
                    nodeCopier.copyTo(insnNode, insnList);
//                    }
                }

                index++;
            }

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

            insnList = new InsnList();

            NodeCopier nodeCopier = new NodeCopier(methodNode.instructions);

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
                    AbstractInsnNode newNode = redirectLocalMethod(methodNode, (MethodInsnNode) insnNode, index);
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
                        newNode = redirectLocalField(methodNode, (FieldInsnNode) insnNode, index);
                        if (newNode != null) {
                            nodeCopier.copyTo(newNode, insnList);
                        } else {
                            nodeCopier.copyTo(insnNode, insnList);
                        }
                    }
                } else {
//                    // Stop once we get to the return
//                    if (insnNode.getOpcode() == RETURN) {
//                        maxLocals += methodNode.maxLocals;
//                        maxStack += methodNode.maxStack;
//                        break;
//                    } else {
                    nodeCopier.copyTo(insnNode, insnList);
//                    }
                }

                index++;
            }

            insnList.accept(methodVisitor);

            methodVisitor.visitMaxs(methodNode.maxStack, methodNode.maxLocals);
            methodVisitor.visitEnd();
        }
    }

    private AbstractInsnNode redirectSuperCall(MethodNode methodNode, FieldInsnNode fieldNode, int index) {
        if (fieldNode.name.equals("_super") && fieldNode.getOpcode() == GETFIELD) {
            AbstractInsnNode nextNode = methodNode.instructions.get(index + 1);

            // We're for certain handling usage of the _super field
            if (nextNode.getOpcode() == Opcodes.CHECKCAST) {
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

    private AbstractInsnNode redirectLocalField(MethodNode methodNode, FieldInsnNode fieldNode, int index) {
        if (fieldNode.owner.equals(templateType)) {
            return ASMUtils.redirect(fieldNode, subType);
        } else {
            return null;
        }
    }

    private AbstractInsnNode redirectLocalMethod(MethodNode methodNode, MethodInsnNode methodInsnNode, int index) {
        if (methodInsnNode.owner.equals(templateType)) {
            return ASMUtils.redirect(methodInsnNode, subType);
        } else {
            return null;
        }
    }
}
