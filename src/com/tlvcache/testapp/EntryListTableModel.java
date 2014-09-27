package com.tlvcache.testapp;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model to represent list of key-value entries
 */
public class EntryListTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	
	List<Entry> settingsList = new ArrayList<>();
	
	@Override
    public int getRowCount() {
        return settingsList.size();
    }
    
    
    @Override
    public int getColumnCount() {
        return 2;
    }
    
    
    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return settingsList.get(row).key;
        } else {
            return settingsList.get(row).value;
        }
    }
    
    
    static class Entry implements Comparable<Entry> {
    	
    	String key;
    	String value;
		
    	public Entry(String key, String value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public int compareTo(Entry o) {
			int keyCompareResult = key.compareTo(o.key);
			if (keyCompareResult != 0) {
				return keyCompareResult;
			} 
			return value.compareTo(o.value);
		}
    }
}
