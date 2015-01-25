package dmillerw.asm;

import com.google.common.collect.Maps;

import java.util.Map;

public abstract class Template<T> {

    public static Map<Integer, Template<?>> templateMap = Maps.newHashMap();

    public static int addTemplate(Template<?> template) {
        int id = templateMap.size();
        templateMap.put(id, template);
        return id;
    }

    public static Template<?> getTemplate(int identifier) {
        return templateMap.get(identifier);
    }

    public T _super;
}
