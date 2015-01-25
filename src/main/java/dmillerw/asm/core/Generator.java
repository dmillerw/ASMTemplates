package dmillerw.asm.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dmillerw.asm.annotation.MConstructor;
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
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class Generator {

    private static final Logger LOGGER = LogManager.getLogger("ASMTemplates");

    private static final ASMClassLoader LOADER = new ASMClassLoader();

    private static final boolean DEBUG = true;

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
        final Set<Mapping> superConstructors = Sets.newHashSet();
        // Mappings of all valid constructors found in the template
        final Set<Mapping> templateConstructors = Sets.newHashSet();

        // All collected method nodes. Can be for constructors, overrides, or implementations
        final Map<Mapping, MethodNode> methodNodes = Maps.newHashMap();

        // All methods that the template will override
        final Set<Mapping> overrideMethods = Sets.newHashSet();

        // All methods that will be implemented in the sub-class from the template
        final Set<Mapping> implementMethods = Sets.newHashSet();

        // Collect all constructors DECLARED directly in the super class
        for (Constructor constructor : superclazz.getDeclaredConstructors()) {
            Mapping mapping = new Mapping(constructor);
            superConstructors.add(mapping);
            debug("Found super-class constructor: " + mapping.toString());
        }

        // Start scanning for annotated methods in the template
        for (Method method : template.getDeclaredMethods()) {
            MConstructor mConstructor = method.getAnnotation(MConstructor.class);
            MOverride mOverride = method.getAnnotation(MOverride.class);
            MImplement mImplement = method.getAnnotation(MImplement.class);

            if (mConstructor != null) {
                Mapping mapping = new Mapping(method);

                mapping.signature = mapping.signature.substring(0, mapping.signature.length() - 1);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(mapping.name) && methodNode.desc.equals(mapping.signature + "V")) {
                        methodNodes.put(mapping, methodNode);
                    }
                }

                debug("Found template constructor: " + mapping.toString());

                templateConstructors.add(mapping);
            }

            if (mOverride != null) {
                Mapping mapping = new Mapping(method);

                // We're overriding a method. Make sure the superclass actually has it
                try {
                    superclazz.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("Cannot override " + method.getName() + " from " + superType + " as it doesn't exist");
                }

                overrideMethods.add(mapping);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(mapping.name) && methodNode.desc.equals(mapping.signature)) {
                        methodNodes.put(mapping, methodNode);
                    }
                }

                // Also grab the method node from the super class and store
                Mapping defMapping = new Mapping("default_" + mapping.name, mapping.signature);
                for (MethodNode methodNode : superNode.methods) {
                    if (methodNode.name.equals(mapping.name) && methodNode.desc.equals(mapping.signature)) {
                        methodNodes.put(defMapping, methodNode);
                    }
                }
            }

            if (mImplement != null) {
                Mapping mapping = new Mapping(method);

                implementMethods.add(mapping);

                for (MethodNode methodNode : templateNode.methods) {
                    if (methodNode.name.equals(mapping.name) && methodNode.desc.equals(mapping.signature)) {
                        methodNodes.put(mapping, methodNode);
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

        // Constructor handling
        for (Mapping mapping : superConstructors) {
            visitor = writer.visitMethod(ACC_PUBLIC, "<init>", mapping.signature + "V", null, null);
            visitor.visitCode();
            visitor.visitVarInsn(ALOAD, 0);
            for (int i=0; i<mapping.params.length; i++) {
                visitor.visitVarInsn(ASMUtils.getLoadCode(mapping.params[i]), i + 1);
            }
            visitor.visitMethodInsn(INVOKESPECIAL, superType, "<init>", mapping.signature + "V", false);

            int maxLocals = mapping.params.length + 2;
            int maxStack = mapping.params.length + 1;

            // If the template has the same constructor
            // We loop because the constructors found in template are proper methods, and have names
            for (Mapping mapping1 : templateConstructors) {
                if (mapping1.signature.equals(mapping.signature)) {
                    debug("Found matching super constructor in template: " + mapping1);

                    MethodNode methodNode = methodNodes.get(mapping1);
                    Iterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
                    while (iterator.hasNext()) {
                        AbstractInsnNode insnNode = iterator.next();

                        // Stop once we get to the return
                        if (insnNode.getOpcode() == RETURN) {
                            maxLocals += methodNode.maxLocals;
                            maxStack += methodNode.maxStack;
                            break;
                        } else {
                            insnNode.accept(visitor);
                        }
                    }
                }
            }

            visitor.visitInsn(RETURN);
            visitor.visitMaxs(maxLocals, maxStack);
            visitor.visitEnd();
        }

        // Method implementations - copying and overriding
        for (Mapping mapping : overrideMethods) {
            MethodNode methodNode = methodNodes.get(mapping);
            Mapping defMapping = new Mapping("default_" + mapping.name, mapping.signature);
            MethodNode defNode = methodNodes.get(defMapping);

            // Generate a new method that contains the super-class method instructions
            visitor = writer.visitMethod(ACC_PUBLIC, "default_" + defNode.name, defNode.desc, null, null);
            visitor.visitCode();

            Iterator<AbstractInsnNode> iterator = defNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();

                // Stop once we get to the return
                if (insnNode.getOpcode() == RETURN) {
                    break;
                } else {
                    if (insnNode instanceof LabelNode || insnNode instanceof LineNumberNode)
                        continue;

                    insnNode.accept(visitor);
                }
            }

            visitor.visitInsn(RETURN);
            visitor.visitMaxs(defNode.maxStack, defNode.maxLocals);
            visitor.visitEnd();

            // Then generate the override method
            visitor = writer.visitMethod(ACC_PUBLIC, mapping.name, mapping.signature, null, null);
            visitor.visitCode();

            InsnList insnList = new InsnList();

            for (int i = 0; i < methodNode.instructions.size(); i++) {
                AbstractInsnNode node = methodNode.instructions.get(i);

                // If we find access to the template's _super field, turn it into a proper super call
                if (node instanceof FieldInsnNode && ((FieldInsnNode) node).name.equals("_super") && node.getOpcode() == GETFIELD) {
                    MethodInsnNode oldNode = (MethodInsnNode) methodNode.instructions.get(i + 2);
                    i += 3; // Skip the next two nodes
                    insnList.add(new VarInsnNode(ALOAD, 0));

                    Mapping oldMapping = new Mapping(oldNode.name, oldNode.desc);

                    // If there's a super call to a method that's been overridden, pass it through
                    // to the generated default method
                    if (overrideMethods.contains(oldMapping)) {
                        debug("Found super call to overridden method!");
                        // Fun fact. This somehow handles super super super methods and I don't even know how
                        insnList.add(new MethodInsnNode(INVOKESPECIAL, clazzDesc, "default_" + oldNode.name, oldNode.desc, false));
                    } else {
                        insnList.add(new MethodInsnNode(INVOKESPECIAL, superType, oldNode.name, oldNode.desc, false));
                    }
                } else {
                    // Otherwise, just copy it into the new method
                    insnList.add(node);
                }
            }

            methodNode.instructions.accept(visitor);
            visitor.visitMaxs(mapping.params.length + 1 + methodNode.maxStack, mapping.params.length + 1 + methodNode.maxLocals);

            visitor.visitEnd();
        }

        // Method handling - new implementations
        for (Mapping mapping : implementMethods) {
            MethodNode node = methodNodes.get(mapping);
            String desc = node.desc;

            visitor = writer.visitMethod(node.access, mapping.name, desc, null, null);
            visitor.visitCode();
            node.instructions.accept(visitor);
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
