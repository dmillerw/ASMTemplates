package dmillerw.asm.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MField;
import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class Generator {

    private static final Logger LOGGER = LogManager.getLogger("ASMTemplates");

    private static final ASMClassLoader LOADER = new ASMClassLoader();

    private static final boolean DEBUG = false;

    private static void debug(String msg) {
        if (DEBUG)
            LOGGER.info(msg);
    }

    public static <T> Class<T> generateSubclass(Class<T> superclazz, Class<? extends Template<?>> template) throws Exception {
        // Super class node. Used for protecting the original methods when overridden
        final ClassNode superNode = ASMUtils.getClassNode(superclazz);

        // Template class node. Used for copying methods
        final ClassNode templateNode = ASMUtils.getClassNode(template);

        // Internal class names
        final String superType = Type.getInternalName(superclazz);
        final String clazzName = superclazz.getName() + "_GENERATED_" + template.hashCode();
        final String clazzDesc = clazzName.replace(".", "/");
        final String tempType = Type.getInternalName(template);

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

        // Collect all constructors DECLARED directly in the super class
        for (Constructor constructor : superclazz.getDeclaredConstructors()) {
            MethodMapping methodMapping = new MethodMapping(constructor);
            superConstructors.add(methodMapping);
            debug("Found super-class constructor: " + methodMapping.toString());
        }

        // Start scanning for annotated methods in the template
        for (Method method : template.getDeclaredMethods()) {
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
                    superclazz.getMethod(method.getName(), method.getParameterTypes());
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

        // Scan for all annotated fields in the template
        for (Field field : template.getDeclaredFields()) {
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

        // All interfaces that the template class implements
        Class<?>[] interfaces = template.getInterfaces();
        // The fully qualified type strings for those interfaces
        String[] interfaceStrs = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceStrs[i] = interfaces[i].getName().replace(".", "/");
        }

        ClassWriter writer = new ClassWriter(0);
        MethodVisitor visitor;

        writer.visit(
                V1_6,
                ACC_PUBLIC | ACC_SUPER,
                clazzDesc,
                null,
                superType,
                interfaceStrs
        );

        writer.visitSource(".dynamic", null);

        // Implement all collected fields from the template
        for (FieldNode fieldNode : fieldNodes.values()) {
            writer.visitField(fieldNode.access, fieldNode.name, fieldNode.desc, null, null);
        }

        // Constructor handling
        for (MethodMapping methodMapping : superConstructors) {
            visitor = writer.visitMethod(ACC_PUBLIC, "<init>", methodMapping.signature + "V", null, null);
            visitor.visitCode();
            visitor.visitVarInsn(ALOAD, 0);
            for (int i = 0; i < methodMapping.params.length; i++) {
                visitor.visitVarInsn(ASMUtils.getLoadCode(methodMapping.params[i]), i + 1);
            }
            visitor.visitMethodInsn(INVOKESPECIAL, superType, "<init>", methodMapping.signature + "V", false);

            int maxLocals = methodMapping.params.length + 2;
            int maxStack = methodMapping.params.length + 1;

            InsnList insnList = new InsnList();

            // If the template has the same constructor
            // We loop because the constructors found in template are proper methods, and have names
            for (MethodMapping methodMapping1 : templateConstructors) {
                if (methodMapping1.signature.equals(methodMapping.signature)) {
                    debug("Found matching super constructor in template: " + methodMapping1);

                    MethodNode methodNode = methodNodes.get(methodMapping1);
                    Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
                    while (iterator.hasNext()) {
                        AbstractInsnNode insnNode = iterator.next();

                        // If we find access to a LOCAL field, re-direct it to the sub class
                        if (insnNode instanceof FieldInsnNode && ((FieldInsnNode) insnNode).owner.equals(tempType)) {
                            insnList.add(ASMUtils.redirect((FieldInsnNode) insnNode, clazzDesc));
                            continue;
                        }

                        // Stop once we get to the return
                        if (insnNode.getOpcode() == RETURN) {
                            maxLocals += methodNode.maxLocals;
                            maxStack += methodNode.maxStack;
                            break;
                        } else {
                            insnList.add(insnNode);
                        }
                    }
                }
            }

            insnList.accept(visitor);

            visitor.visitInsn(RETURN);
            visitor.visitMaxs(maxLocals, maxStack);
            visitor.visitEnd();
        }

        // Method implementations - copying and overriding
        for (MethodMapping methodMapping : overrideMethods) {
            MethodNode methodNode = methodNodes.get(methodMapping);
            MethodMapping defMethodMapping = new MethodMapping("default_" + methodMapping.name, methodMapping.signature);
            MethodNode defNode = methodNodes.get(defMethodMapping);

            // Generate a new method that contains the super-class method instructions
            {
                visitor = writer.visitMethod(ACC_PUBLIC, "default_" + defNode.name, defNode.desc, null, null);
                visitor.visitCode();

                InsnList insnList = new InsnList();

                Iterator<AbstractInsnNode> iterator = defNode.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode insnNode = iterator.next();

                    if (insnNode instanceof LabelNode || insnNode instanceof LineNumberNode)
                        continue;

                    insnList.add(insnNode);
                }

                insnList.accept(visitor);

                visitor.visitMaxs(defNode.maxStack, defNode.maxLocals);
                visitor.visitEnd();
            }

            // Then generate the override method
            {
                visitor = writer.visitMethod(ACC_PUBLIC, methodMapping.name, methodMapping.signature, null, null);
                visitor.visitCode();

                InsnList insnList = new InsnList();

                for (int i = 0; i < methodNode.instructions.size(); i++) {
                    AbstractInsnNode node = methodNode.instructions.get(i);

                    if (node instanceof FieldInsnNode) {
                        if (((FieldInsnNode) node).name.equals("_super") && node.getOpcode() == GETFIELD) {
                            //TODO This is hard-coded for super methods
                            //TODO Add support for super fields

                            MethodInsnNode oldNode = (MethodInsnNode) methodNode.instructions.get(i + 2);
                            i += 3; // Skip the next two nodes
                            insnList.add(new VarInsnNode(ALOAD, 0));

                            MethodMapping oldMethodMapping = new MethodMapping(oldNode.name, oldNode.desc);

                            // If there's a super call to a method that's been overridden, pass it through
                            // to the generated default method
                            if (overrideMethods.contains(oldMethodMapping)) {
                                debug("Found super call to overridden method!");
                                // Fun fact. This somehow handles super super super methods and I don't even know how
                                insnList.add(new MethodInsnNode(INVOKESPECIAL, clazzDesc, "default_" + oldNode.name, oldNode.desc, false));
                            } else {
                                insnList.add(new MethodInsnNode(INVOKESPECIAL, superType, oldNode.name, oldNode.desc, false));
                            }
                        } else if (((FieldInsnNode) node).owner.equals(tempType)) {
                            insnList.add(ASMUtils.redirect((FieldInsnNode) node, clazzDesc));
                        } else {
                            insnList.add(node);
                        }
                    } else {
                        // Otherwise, just copy it into the new method
                        insnList.add(node);
                    }
                }

                insnList.accept(visitor);
                visitor.visitMaxs(methodMapping.params.length + 1 + methodNode.maxStack, methodMapping.params.length + 1 + methodNode.maxLocals);

                visitor.visitEnd();
            }
        }

        // Method handling - new implementations
        for (MethodMapping methodMapping : implementMethods) {
            MethodNode node = methodNodes.get(methodMapping);
            String desc = node.desc;

            visitor = writer.visitMethod(node.access, methodMapping.name, desc, null, null);
            visitor.visitCode();

            InsnList insnList = new InsnList();

            for (int i = 0; i < node.instructions.size(); i++) {
                AbstractInsnNode insnNode = node.instructions.get(i);

                if (insnNode instanceof FieldInsnNode) {
                    if (((FieldInsnNode) insnNode).owner.equals(tempType)) {
                        insnList.add(ASMUtils.redirect((FieldInsnNode) insnNode, clazzDesc));
                    } else if (((FieldInsnNode) insnNode).name.equals("_super") && insnNode.getOpcode() == GETFIELD) {
                        MethodInsnNode oldNode = (MethodInsnNode) node.instructions.get(i + 2);
                        i += 3; // Skip the next two nodes
                        insnList.add(new VarInsnNode(ALOAD, 0));

                        MethodMapping oldMethodMapping = new MethodMapping(oldNode.name, oldNode.desc);

                        // If there's a super call to a method that's been overridden, pass it through
                        // to the generated default method
                        if (overrideMethods.contains(oldMethodMapping)) {
                            debug("Found super call to overridden method!");
                            // Fun fact. This somehow handles super super super methods and I don't even know how
                            insnList.add(new MethodInsnNode(INVOKESPECIAL, clazzDesc, "default_" + oldNode.name, oldNode.desc, false));
                        } else {
                            insnList.add(new MethodInsnNode(INVOKESPECIAL, superType, oldNode.name, oldNode.desc, false));
                        }
                    } else {
                        insnList.add(insnNode);
                    }
                } else {
                    // Otherwise, just copy it into the new method
                    insnList.add(insnNode);
                }
            }

            insnList.accept(visitor);

            visitor.visitMaxs(node.maxStack, node.maxLocals);
            visitor.visitEnd();
        }

        writer.visitEnd();

        Class<?> clazz = LOADER.define(clazzName, writer.toByteArray());
        return (Class<T>) clazz;
    }

    private static class ASMClassLoader extends ClassLoader {

        private ASMClassLoader() {
            super(ASMClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, byte[] data) {
            return defineClass(name, data, 0, data.length);
        }
    }
}
