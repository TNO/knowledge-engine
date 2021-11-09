package eu.knowledge.engine.reasoner;

import java.util.Map;
import java.util.Set;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;

public final class DataBindingSetHandler implements BindingSetHandler {
	private Table data;

	public DataBindingSetHandler(Table someData) {
		this.data = someData;
	}

	@Override
	public BindingSet handle(BindingSet bs) {

		BindingSet newBS = new BindingSet();
		if (!bs.isEmpty()) {

			for (Binding b : bs) {

				if (!b.isEmpty()) {
					Set<Map<String, String>> map = data.query(b.toMap());
					if (!map.isEmpty())
						newBS.addAll(map);
				} else {
					newBS.addAll(this.data.getData());
				}
			}
		} else {
			newBS.addAll(this.data.getData());
		}
		return newBS;
	}
}