package eu.knowledge.engine.reasoner.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Table {

	private Set<Map<String, String>> data = new HashSet<>();

	/**
	 * Initialize a table that supports limited querying to make it easier to
	 * process incoming binding sets.
	 * 
	 * @param columns The column names of columns in the table.
	 * @param rows    The rows with the values for each column in the table. Every
	 *                cell value is expected to be separated by a {@code ,} (comma)
	 *                and should be a valid binding set value.
	 */
	public Table(String[] columns, String[] rows) {

		assert columns.length > 0;

		if (rows.length > 0) {
			assert columns.length == rows[0]
					.split(",").length : "Rows should have the same number of values as there are columns.";

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
	}

	/**
	 * Query the data that is initialised in this table. Returns all rows in the
	 * table whose column values match the values for the specified columns in the
	 * input query.
	 * 
	 * @param query The query with zero or more values for specific columns.
	 * @return All rows matching all values from the query are returned.
	 */
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

	/**
	 * @return All data that is stored in this table.
	 */
	public Set<Map<String, String>> getData() {
		return this.data;
	}

}
