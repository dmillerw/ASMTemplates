package dmillerw.asm.core;

import java.lang.reflect.Field;

public class FieldMapping {

    public Class<?> type;

    public String name;
    public String signature;

    public FieldMapping(String name, String signature) {
        this.name = name;
        this.signature = signature;
    }

    public FieldMapping(Field field) {
        this.type = field.getType();
        this.name = field.getName();
        this.signature = ASMUtils.getSignature(this.type);
    }

    @Override
    public String toString() {
        return "{name: " + name + ", signature: " + signature + "}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        FieldMapping that = (FieldMapping) object;

        if (!name.equals(that.name)) return false;
        if (!signature.equals(that.signature)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + signature.hashCode();
        return result;
    }
}
