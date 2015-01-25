package dmillerw.asm.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Mapping {

    public Class<?>[] params;
    public Class<?> returnType;
    public String signature;

    public Mapping(Constructor constructor) {
        this.params = constructor.getParameterTypes();
        this.returnType = null;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (Class<?> clazz : params) {
            stringBuilder.append(ASMUtils.getSignature(clazz));
        }
        stringBuilder.append(")");

        this.signature = stringBuilder.toString();
    }

    public Mapping(Method method) {
        this.params = method.getParameterTypes();
        this.returnType = method.getReturnType();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (Class<?> clazz : params) {
            stringBuilder.append(ASMUtils.getSignature(clazz));
        }
        stringBuilder.append(")");
        if (returnType != null) {
            stringBuilder.append(ASMUtils.getSignature(returnType));
        } else {
            stringBuilder.append("V");
        }

        this.signature = stringBuilder.toString();
    }
}
