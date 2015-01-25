package dmillerw.asm.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dmillerw.asm.annotation.MConstructor;
import dmillerw.asm.annotation.MImplement;
import dmillerw.asm.annotation.MOverride;
import dmillerw.asm.template.Template;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class Generator {

    private static final ASMClassLoader LOADER = new ASMClassLoader();

    public static <T> Class<T> generateSubclass(Class<T> superclazz, Template<?> template) throws Exception {
        final int id = Template.addTemplate(template);

        final ClassNode templateNode = ASMUtils.getClassNode(template.getClass());

        final String superType = Type.getInternalName(superclazz);
        final String clazzName = superclazz.getName() + "_GENERATED_" + template.hashCode();
        final String clazzDesc = clazzName.replace(".", "/");

        final String tempType = Type.getInternalName(template.getClass());

        final List<Mapping> constructors = Lists.newArrayList();
        final Map<String, String> mappingDescToNameMap = Maps.newHashMap();

        for (Constructor constructor : superclazz.getDeclaredConstructors()) {
            Mapping mapping = new Mapping(constructor);
            constructors.add(mapping);
        }

        final Map<String, Mapping> methodMappings = Maps.newHashMap();
        final Map<String, MethodNode> copyMethods = Maps.newHashMap();
        final Map<String, MethodNode> implementMethods = Maps.newHashMap();

        // Start scanning for annotated methods in the template
        for (Method method : template.getClass().getDeclaredMethods()) {
            MConstructor mConstructor = method.getAnnotation(MConstructor.class);
            MOverride mOverride = method.getAnnotation(MOverride.class);
            MImplement mImplement = method.getAnnotation(MImplement.class);

            if (mConstructor != null) {
                Mapping mapping = new Mapping(method);
                String sig = mapping.signature.replace("V", "");
                mappingDescToNameMap.put(sig, method.getName());
            }

            if (mOverride != null) {
                Mapping mapping = new Mapping(method);

                // We're overriding a method. Make sure the superclass actually has it
                try {
                    superclazz.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("Cannot override " + method.getName() + " from " + superType + " as it doesn't exist");
                }

                methodMappings.put(method.getName(), mapping);

                if (templateNode != null) {
                    if (mOverride.copy()) {
                        for (MethodNode methodNode : templateNode.methods) {
                            if (methodNode.name.equals(method.getName()) && methodNode.desc.equals(mapping.signature)) {
                                copyMethods.put(methodNode.name, methodNode);
                            }
                        }
                    }
                }
            }

            if (mImplement != null) {
                if (templateNode != null) {
                    Mapping mapping = new Mapping(method);
                    for (MethodNode methodNode : templateNode.methods) {
                        if (methodNode.name.equals(method.getName()) && methodNode.desc.equals(mapping.signature)) {
                            implementMethods.put(methodNode.name, methodNode);
                        }
                    }
                }
            }
        }

        Class<?>[] interfaces = template.getClass().getInterfaces();
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

        writer.visitField(ACC_PROTECTED, "_template", "Ldmillerw/asm/template/Template;", null, null);

        // Constructor handling
        for (Mapping mapping : constructors) {
            visitor = writer.visitMethod(ACC_PUBLIC, "<init>", mapping.signature + "V", null, null);
            visitor.visitCode();
            visitor.visitVarInsn(ALOAD, 0);
            for (int i=0; i<mapping.params.length; i++) {
                visitor.visitVarInsn(ASMUtils.getLoadCode(mapping.params[i]), i + 1);
            }
            visitor.visitMethodInsn(INVOKESPECIAL, superType, "<init>", mapping.signature + "V", false);

            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitLdcInsn(id);
            visitor.visitMethodInsn(INVOKESTATIC, "dmillerw/asm/template/Template", "getTemplate", "(I)Ldmillerw/asm/template/Template;", false);
            visitor.visitFieldInsn(PUTFIELD, clazzDesc, "_template", "Ldmillerw/asm/template/Template;");

            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitFieldInsn(GETFIELD, clazzDesc, "_template", "Ldmillerw/asm/template/Template;");
            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitFieldInsn(PUTFIELD, "dmillerw/asm/template/Template", "_super", "Ljava/lang/Object;");

            if (mappingDescToNameMap.containsKey(mapping.signature)) {
                String name = mappingDescToNameMap.get(mapping.signature);
                visitor.visitVarInsn(ALOAD, 0);
                visitor.visitFieldInsn(GETFIELD, clazzDesc, "_template", "Ldmillerw/asm/template/Template;");
                visitor.visitTypeInsn(CHECKCAST, tempType);
                for (int i=0; i<mapping.params.length; i++) {
                    visitor.visitVarInsn(ASMUtils.getLoadCode(mapping.params[i]), i + 1);
                }
                visitor.visitMethodInsn(INVOKEVIRTUAL, tempType, name, mapping.signature + "V", false);
            }

            visitor.visitInsn(RETURN);
            visitor.visitMaxs(mapping.params.length + 2, mapping.params.length + 1);
            visitor.visitEnd();
        }

        // Method implementations - copying and overidding
        for (Map.Entry<String, Mapping> entry : methodMappings.entrySet()) {
            String name = entry.getKey();
            Mapping mapping = entry.getValue();

            visitor = writer.visitMethod(ACC_PUBLIC, name, mapping.signature, null, null);
            visitor.visitCode();

            if (copyMethods.containsKey(name)) {
                MethodNode methodNode = copyMethods.get(name);

                InsnList insnList = new InsnList();

                for (int i = 0; i < methodNode.instructions.size(); i++) {
                    AbstractInsnNode node = methodNode.instructions.get(i);

                    // If we find access to the template's _super field, turn it into a proper super call
                    if (node instanceof FieldInsnNode && ((FieldInsnNode) node).name.equals("_super") && node.getOpcode() == GETFIELD) {
                        MethodInsnNode oldNode = (MethodInsnNode) methodNode.instructions.get(i + 2);
                        i += 3; // Skip the next two nodes
                        insnList.add(new VarInsnNode(ALOAD, 0));
                        insnList.add(new MethodInsnNode(INVOKESPECIAL, superType, oldNode.name, oldNode.desc, false));
                    } else {
                        insnList.add(node);
                    }
                }

                methodNode.instructions.accept(visitor);
                visitor.visitMaxs(mapping.params.length + 1 + methodNode.maxStack, mapping.params.length + 1 + methodNode.maxLocals);
            } else {
                visitor.visitVarInsn(ALOAD, 0);
                visitor.visitFieldInsn(GETFIELD, clazzDesc, "_template", "Ldmillerw/asm/template/Template;");
                visitor.visitTypeInsn(CHECKCAST, tempType);
                for (int i=0; i<mapping.params.length; i++) {
                    visitor.visitVarInsn(ASMUtils.getLoadCode(mapping.params[i]), i + 1);
                }
                visitor.visitMethodInsn(INVOKEVIRTUAL, tempType, name, mapping.signature, false);
                visitor.visitInsn(ASMUtils.getReturnCode(mapping.returnType));
                visitor.visitMaxs(mapping.params.length + 1, mapping.params.length + 1);
            }

            visitor.visitEnd();
        }

        // Method handling - new implementations
        for (Map.Entry<String, MethodNode> entry : implementMethods.entrySet()) {
            String name = entry.getKey();
            MethodNode node = entry.getValue();
            String desc = node.desc;

            visitor = writer.visitMethod(node.access, name, desc, null, null);
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
