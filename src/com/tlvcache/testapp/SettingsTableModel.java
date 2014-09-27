package com.tlvcache.testapp;

import java.util.Properties;


/**
 * Table model to represent cache configuration params
 */
class SettingsTableModel extends EntryListTableModel {
    
	private static final long serialVersionUID = 1L;
	

	SettingsTableModel(Properties settings) {
		setProperties(settings);
	}
	
	
	Properties getAsProperties() {
		Properties result = new Properties();
		for (Entry e : settingsList) {
			result.put(e.key, e.value);
		}
		return result;
	}
	
	
	void setProperties(Properties settings) {
		settingsList.clear();
		for (Object key : settings.keySet()) {
			settingsList.add(new Entry(key.toString(), settings.get(key).toString()));
		}
	}
	
	
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
    	return columnIndex == 1;
    }
    
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    	settingsList.get(rowIndex).value = aValue.toString();
    }
    
    
    Properties getSettingsAsProperties() {
    	Properties result = new Properties();
    	for (Entry entry : settingsList) {
    		result.put(entry.key, entry.value);
    	}
    	return result;
    }
}