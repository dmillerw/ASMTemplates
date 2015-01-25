package dmillerw.asm.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MethodMapping {

    public Class<?>[] params;
    public Class<?> returnType;

    public String name;
    public String signature;

    public MethodMapping(String name, String signature) {
        this.name = name;
        this.signature = signature;
    }

    public MethodMapping(Constructor constructor) {
        this.params = constructor.getParameterTypes();
        this.returnType = null;

        this.name = "<init>";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        for (Class<?> clazz : params) {
            stringBuilder.append(ASMUtils.getSignature(clazz));
        }
        stringBuilder.append(")");

        this.signature = stringBuilder.toString();
    }

    public MethodMapping(Method method) {
        this.params = method.getParameterTypes();
        this.returnType = method.getReturnType();

        this.name = method.getName();

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

    @Override
    public String toString() {
        return "{name: " + name + ", signature: " + signature + "}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        MethodMapping methodMapping = (MethodMapping) object;

        if (!name.equals(methodMapping.name)) return false;
        if (!signature.equals(methodMapping.signature)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + signature.hashCode();
        return result;
    }
}
