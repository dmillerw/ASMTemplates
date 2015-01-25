package dmillerw.asm.core;

import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

public class ASMUtils {

    public static int getLoadCode(Class<?> clazz) {
        if (clazz == byte.class) {
            return Opcodes.ILOAD;
        } else if (clazz == boolean.class) {
            return Opcodes.ILOAD;
        } else if (clazz == short.class) {
            return Opcodes.ILOAD;
        } else if (clazz == int.class) {
            return Opcodes.ILOAD;
        } else if (clazz == long.class) {
            return Opcodes.LLOAD;
        } else if (clazz == float.class) {
            return Opcodes.FLOAD;
        } else if (clazz == double.class) {
            return Opcodes.DLOAD;
        } else {
            return Opcodes.ALOAD;
        }
    }

    public static int getReturnCode(Class<?> clazz) {
        if (clazz == byte.class) {
            return Opcodes.IRETURN;
        } else if (clazz == boolean.class) {
            return Opcodes.IRETURN;
        } else if (clazz == short.class) {
            return Opcodes.IRETURN;
        } else if (clazz == int.class) {
            return Opcodes.IRETURN;
        } else if (clazz == long.class) {
            return Opcodes.LRETURN;
        } else if (clazz == float.class) {
            return Opcodes.FRETURN;
        } else if (clazz == double.class) {
            return Opcodes.DRETURN;
        } else {
            return Opcodes.ARETURN;
        }
    }
    
    public static ClassNode getClassNode(Class<?> clazz) throws IOException {
        ClassNode cnode = new ClassNode();
        ClassReader reader = new ClassReader(Launch.classLoader.getClassBytes(clazz.getName()));
        reader.accept(cnode, 0);
        return cnode;
    }

    public static String getSignature(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == byte.class) {
                return "B";
            } else if (clazz == boolean.class) {
                return "Z";
            } else if (clazz == short.class) {
                return "S";
            } else if (clazz == int.class) {
                return "I";
            } else if (clazz == long.class) {
                return "L";
            } else if (clazz == float.class) {
                return "F";
            } else if (clazz == double.class) {
                return "D";
            }
        } else {
            if (clazz.isArray()) {
                return "[L" + clazz.getComponentType().getName().replace(".", "/") + ";";
            } else {
                return "L" + clazz.getName().replace(".", "/") + ";";
            }
        }

        return "";
    }
}
