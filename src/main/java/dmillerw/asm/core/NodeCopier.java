package dmillerw.asm.core;

import com.google.common.collect.Maps;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

public class NodeCopier {

    private InsnList sourceList;

    private Map<LabelNode, LabelNode> labelMap;

    public NodeCopier(InsnList sourceList) {
        this.sourceList = sourceList;
        this.labelMap = Maps.newHashMap();

        // build the label map
        for (AbstractInsnNode instruction = sourceList.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof LabelNode) {
                labelMap.put(((LabelNode) instruction), new LabelNode());
            }
        }
    }

    public void copyTo(AbstractInsnNode node, InsnList destination) {
        if (node == null)
            return;

        if (destination == null)
            return;

        destination.add(node.clone(labelMap));
    }
}
