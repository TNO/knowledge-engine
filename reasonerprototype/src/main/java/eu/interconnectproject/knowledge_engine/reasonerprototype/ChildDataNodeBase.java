package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;

import eu.interconnectproject.knowledge_engine.reasonerprototype.ChildDataNodeBase.ChildData;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.BindingSet;

/**
 * Utility class that is used by nodes that have multiple children.
 */
public class ChildDataNodeBase<RN extends ReasoningNode> extends ArrayList<ChildData<RN>> {

	private static final long serialVersionUID = 6518546322917038117L;

	public static class ChildData<RN> {
		private final RN child;
		private BindingSet result;

		public ChildData(RN child) {
			this.child = child;
		}

		public RN getChild() {
			return child;
		}

		public void setResult(BindingSet result) {
			this.result = result;
		}

		public BindingSet getResult() {
			return result;
		}
	}

	public ChildData<RN> getChildData(RN child) {
		return this.stream().filter(cd -> cd.getChild() == child).findAny().orElse(null);
	}

	public boolean allChildrenFinished() {
		for (ChildData<RN> child : this) {
			if (child.getResult() == null) {
				return false;
			}
		}
		return true;
	}

	public void add(RN childNode) {
		this.add(new ChildData<RN>(childNode));
	}

	public BindingSet getMergedBindingSet() {
		BindingSet bs = new BindingSet();
		for (ChildData<RN> cd : this) {
			bs = bs.merge(cd.getResult());
		}
		return bs;
	}

}
