package eu.knowledge.engine.reasoner2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Table {

	private Set<Map<String, String>> data = new HashSet<>();

	public Table(String[] columns, String[] rows) {

		Map<String, String> newRow;
		for (String row : rows) {
			newRow = new HashMap<>();
			String[] values = row.split(",");

			for (int i = 0; i < values.length; i++) {
				String val = values[i];
				newRow.put(columns[i], val);
			}
			data.add(newRow);
		}
	}

	public Set<Map<String, String>> query(Map<String, String> query) {

		Set<Map<String, String>> result = new HashSet<>();
		for (Map<String, String> row : data) {

			boolean match = true;

			for (Map.Entry<String, String> entry : query.entrySet()) {
				if (!row.containsKey(entry.getKey()) || !row.get(entry.getKey()).equals(entry.getValue())) {
					match = false;
				}
			}

			if (match) {
				result.add(row);
			}
		}
		return result;
	}

	public Set<Map<String, String>> getData() {
		return this.data;
	}

}
