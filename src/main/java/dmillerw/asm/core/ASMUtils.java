package dmillerw.asm.core;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class ASMUtils {

    private static Map<Integer, String> opCodes = Maps.newHashMap();

    private static void put(String str, int code) {
        opCodes.put(code, str);
    }

    static {
        put("F_NEW", F_NEW);
        put("NOP", NOP);
        put("ACONST_NULL", ACONST_NULL);
        put("ICONST_M1", ICONST_M1);
        put("ICONST_0", ICONST_0);
        put("ICONST_1", ICONST_1);
        put("ICONST_2", ICONST_2);
        put("ICONST_3", ICONST_3);
        put("ICONST_4", ICONST_4);
        put("ICONST_5", ICONST_5);
        put("LCONST_0", LCONST_0);
        put("LCONST_1", LCONST_1);
        put("FCONST_0", FCONST_0);
        put("FCONST_1", FCONST_1);
        put("FCONST_2", FCONST_2);
        put("DCONST_0", DCONST_0);
        put("DCONST_1", DCONST_1);
        put("BIPUSH", BIPUSH);
        put("SIPUSH", SIPUSH);
        put("LDC", LDC);
        put("ILOAD", ILOAD);
        put("LLOAD", LLOAD);
        put("FLOAD", FLOAD);
        put("DLOAD", DLOAD);
        put("ALOAD", ALOAD);
        put("IALOAD", IALOAD);
        put("LALOAD", LALOAD);
        put("FALOAD", FALOAD);
        put("DALOAD", DALOAD);
        put("AALOAD", AALOAD);
        put("BALOAD", BALOAD);
        put("CALOAD", CALOAD);
        put("SALOAD", SALOAD);
        put("ISTORE", ISTORE);
        put("LSTORE", LSTORE);
        put("FSTORE", FSTORE);
        put("DSTORE", DSTORE);
        put("ASTORE", ASTORE);
        put("IASTORE", IASTORE);
        put("LASTORE", LASTORE);
        put("FASTORE", FASTORE);
        put("DASTORE", DASTORE);
        put("AASTORE", AASTORE);
        put("BASTORE", BASTORE);
        put("CASTORE", CASTORE);
        put("SASTORE", SASTORE);
        put("POP", POP);
        put("POP2", POP2);
        put("DUP", DUP);
        put("DUP_X1", DUP_X1);
        put("DUP_X2", DUP_X2);
        put("DUP2", DUP2);
        put("DUP2_X1", DUP2_X1);
        put("DUP2_X2", DUP2_X2);
        put("SWAP", SWAP);
        put("IADD", IADD);
        put("LADD", LADD);
        put("FADD", FADD);
        put("DADD", DADD);
        put("ISUB", ISUB);
        put("LSUB", LSUB);
        put("FSUB", FSUB);
        put("DSUB", DSUB);
        put("IMUL", IMUL);
        put("LMUL", LMUL);
        put("FMUL", FMUL);
        put("DMUL", DMUL);
        put("IDIV", IDIV);
        put("LDIV", LDIV);
        put("FDIV", FDIV);
        put("DDIV", DDIV);
        put("IREM", IREM);
        put("LREM", LREM);
        put("FREM", FREM);
        put("DREM", DREM);
        put("INEG", INEG);
        put("LNEG", LNEG);
        put("FNEG", FNEG);
        put("DNEG", DNEG);
        put("ISHL", ISHL);
        put("LSHL", LSHL);
        put("ISHR", ISHR);
        put("LSHR", LSHR);
        put("IUSHR", IUSHR);
        put("LUSHR", LUSHR);
        put("IAND", IAND);
        put("LAND", LAND);
        put("IOR", IOR);
        put("LOR", LOR);
        put("IXOR", IXOR);
        put("LXOR", LXOR);
        put("IINC", IINC);
        put("I2L", I2L);
        put("I2F", I2F);
        put("I2D", I2D);
        put("L2I", L2I);
        put("L2F", L2F);
        put("L2D", L2D);
        put("F2I", F2I);
        put("F2L", F2L);
        put("F2D", F2D);
        put("D2I", D2I);
        put("D2L", D2L);
        put("D2F", D2F);
        put("I2B", I2B);
        put("I2C", I2C);
        put("I2S", I2S);
        put("LCMP", LCMP);
        put("FCMPL", FCMPL);
        put("FCMPG", FCMPG);
        put("DCMPL", DCMPL);
        put("DCMPG", DCMPG);
        put("IFEQ", IFEQ);
        put("IFNE", IFNE);
        put("IFLT", IFLT);
        put("IFGE", IFGE);
        put("IFGT", IFGT);
        put("IFLE", IFLE);
        put("IF_ICMPEQ", IF_ICMPEQ);
        put("IF_ICMPNE", IF_ICMPNE);
        put("IF_ICMPLT", IF_ICMPLT);
        put("IF_ICMPGE", IF_ICMPGE);
        put("IF_ICMPGT", IF_ICMPGT);
        put("IF_ICMPLE", IF_ICMPLE);
        put("IF_ACMPEQ", IF_ACMPEQ);
        put("IF_ACMPNE", IF_ACMPNE);
        put("GOTO", GOTO);
        put("JSR", JSR);
        put("RET", RET);
        put("TABLESWITCH", TABLESWITCH);
        put("LOOKUPSWITCH", LOOKUPSWITCH);
        put("IRETURN", IRETURN);
        put("LRETURN", LRETURN);
        put("FRETURN", FRETURN);
        put("DRETURN", DRETURN);
        put("ARETURN", ARETURN);
        put("RETURN", RETURN);
        put("GETSTATIC", GETSTATIC);
        put("PUTSTATIC", PUTSTATIC);
        put("GETFIELD", GETFIELD);
        put("PUTFIELD", PUTFIELD);
        put("INVOKEVIRTUAL", INVOKEVIRTUAL);
        put("INVOKESPECIAL", INVOKESPECIAL);
        put("INVOKESTATIC", INVOKESTATIC);
        put("INVOKEINTERFACE", INVOKEINTERFACE);
        put("INVOKEDYNAMIC", INVOKEDYNAMIC);
        put("NEW", NEW);
        put("NEWARRAY", NEWARRAY);
        put("ANEWARRAY", ANEWARRAY);
        put("ARRAYLENGTH", ARRAYLENGTH);
        put("ATHROW", ATHROW);
        put("CHECKCAST", CHECKCAST);
        put("INSTANCEOF", INSTANCEOF);
        put("MONITORENTER", MONITORENTER);
        put("MONITOREXIT", MONITOREXIT);
        put("MULTIANEWARRAY", MULTIANEWARRAY);
        put("IFNULL", IFNULL);
        put("IFNONNULL", IFNONNULL);
    }

    public static String opcodeToString(int opcode) {
        return opCodes.get(opcode);
    }

    public static String nodeToString(AbstractInsnNode node) {
        if (node instanceof FieldInsnNode) {
            return opcodeToString(node.getOpcode()) + " {owner: " + ((FieldInsnNode) node).owner + ", name: " + ((FieldInsnNode) node).name + ", desc: " + ((FieldInsnNode) node).desc + "}";
        } else if (node instanceof MethodInsnNode) {
            return opcodeToString(node.getOpcode()) + " {owner: " + ((MethodInsnNode) node).owner + ", name: " + ((MethodInsnNode) node).name + ", desc: " + ((MethodInsnNode) node).desc + "}";
        } else if (node instanceof VarInsnNode) {
            return opcodeToString(node.getOpcode()) + " " + Integer.toString(((VarInsnNode) node).var);
        } else if (node instanceof LdcInsnNode) {
            return opcodeToString(node.getOpcode()) + " " + ((LdcInsnNode) node).cst.toString();
        } else if (node instanceof TypeInsnNode) {
            return opcodeToString(node.getOpcode()) + " " + ((TypeInsnNode) node).desc;
        } else {
            String opcode = opcodeToString(node.getOpcode());
            return opcode == null ? Integer.toString(node.getOpcode()) : opcode;
        }
    }

    public static String insnListToString(InsnList insnList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < insnList.size(); i++) {
            AbstractInsnNode node = insnList.get(i);
            if (node != null) {
                stringBuilder.append(i).append(": ").append(ASMUtils.nodeToString(node)).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    public static void accept(MethodVisitor methodVisitor, LinkedList<AbstractInsnNode> insnNodes) {
        for (AbstractInsnNode node : insnNodes) {
            if (node != null) {
                node.accept(methodVisitor);
            }
        }
    }

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
        if (clazz == null || clazz == void.class) {
            return Opcodes.RETURN;
        }

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

    public static ClassNode getClassNode(Class<?> clazz) {
        try {
            String name = clazz.getName().replace(".", "/") + ".class";
            byte[] data = IOUtils.toByteArray(clazz.getClassLoader().getResourceAsStream(name));

            ClassNode cnode = new ClassNode();
            ClassReader reader = new ClassReader(data);
            reader.accept(cnode, 0);

            return cnode;
        } catch (IOException ignore) {
            return null;
        }
    }

    public static InsnList copyInsnList(InsnList insnList) {
        InsnList clone = new InsnList();

        // used to map the old labels to their cloned counterpart
        Map<LabelNode, LabelNode> labelMap = Maps.newHashMap();

        // build the label map
        for (AbstractInsnNode instruction = insnList.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof LabelNode) {
                labelMap.put(((LabelNode) instruction), new LabelNode());
            }
        }

        for (AbstractInsnNode instruction = insnList.getFirst(); instruction != null; instruction = instruction.getNext()) {
            clone.add(instruction.clone(labelMap));
        }

        return clone;
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

        return "V";
    }

    public static FieldInsnNode redirect(FieldInsnNode node, String owner) {
        return new FieldInsnNode(node.getOpcode(), owner, node.name, node.desc);
    }

    public static MethodInsnNode redirect(MethodInsnNode node, String owner) {
        return new MethodInsnNode(node.getOpcode(), owner, node.name, node.desc, node.itf);
    }
}
