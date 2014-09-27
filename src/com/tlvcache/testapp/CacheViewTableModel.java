package com.tlvcache.testapp;

import java.util.Collections;
import java.util.Map;

/**
 * Table model to represent cache content as list of key-value entries
 */
public class CacheViewTableModel extends EntryListTableModel {

	private static final long serialVersionUID = 1L;

	/**
	 * Sets representing data
	 * @param data Map of key-value entries
	 */
	void setData(Map<String, Object> data) {
		settingsList.clear();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			settingsList.add(new Entry(entry.getKey(), entry.getValue().toString()));
		}
		Collections.sort(settingsList);
		fireTableDataChanged();
	}
}
