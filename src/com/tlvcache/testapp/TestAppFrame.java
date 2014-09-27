package com.tlvcache.testapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.tlvcache.TlvCacheInstance;
import com.tlvcache.TlvCacheState;

/**
 * Main frame for test application
 */
public class TestAppFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Application data model
	 */
	private final TestAppModel cacheModel;
	
	
	//===================================================================================
	//----------------- FRAME COMPONENTS ------------------------------------------------
	//===================================================================================
	
	
	JSplitPane splitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
    
		JSplitPane splitPaneLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		JPanel panelLeftPart = new JPanel();
		
			JScrollPane scrollPaneTableSettings = new JScrollPane();
				JTable tableSettings = new JTable();
			
			JPanel panelCacheManage = new JPanel();
				JButton buttonLoadSettings = new JButton("Load configuration");
				JButton buttonSaveSettings = new JButton("Save configuration");
	            JButton buttonInitCache = new JButton("Init cache");
	            JButton buttonStartCache = new JButton("Start cache");
	            JButton buttonStopCache = new JButton("Stop cache");
	            JButton buttonShutdownCache = new JButton("Shutdown cache");         
            
		
        JSplitPane splitPaneCacheControl = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		
        	JPanel panelCacheContent = new JPanel();
    
        		JPanel panelCacheState = new JPanel();
        			JLabel labelCacheState = new JLabel("");
        			JButton buttonUpdateView = new JButton("Update view");
        	
				JPanel panelCacheEdit = new JPanel();
				
					JPanel panelKeys = new JPanel();
						JLabel labelKey = new JLabel("Key");
						JPanel panelKeyFields = new JPanel();
							JTextField fieldKeyPut = new JTextField();
							JTextField fieldKeyGet = new JTextField();
							JTextField fieldKeyRemove = new JTextField();
							
					JPanel panelValues = new JPanel();
						JLabel labelValue = new JLabel("Value");
						JPanel panelValueFields = new JPanel();
							JScrollPane scrollPaneValuePut = new JScrollPane();
								JTextArea textAreaValuePut = new JTextArea();
							JScrollPane scrollPaneValueGet = new JScrollPane();
								JTextArea textAreaValueGet = new JTextArea();
							JScrollPane scrollPaneValueRemove = new JScrollPane();
								JTextArea textAreaValueRemove = new JTextArea();
							
					JPanel panelEditButtons = new JPanel();
						JLabel labelButtons = new JLabel("");
						JPanel panelEditButtonsGrid = new JPanel();
							JButton buttonPut = new JButton("Put");
							JButton buttonGet = new JButton("Get");
							JButton buttonRemove = new JButton("Remove");
				
				JPanel panelCacheView = new JPanel();
					
					JPanel panelMemoryCacheContent = new JPanel();
						JLabel labelMemoryCache = new JLabel("Memory cache");
						JScrollPane scrollPaneMemoryCache = new JScrollPane();
							JTable tableMemoryCache = new JTable();
					
					JPanel panelFsCacheContent = new JPanel();
						JLabel labelFsCache = new JLabel("Filesystem cache");
						JScrollPane scrollPaneFsCache = new JScrollPane();
							JTable tableFsCache = new JTable();
        
			JPanel panelCacheLog = new JPanel();
				JScrollPane scrollPaneCacheLog = new JScrollPane();
					JTextArea textAreaCacheLog = new JTextArea();
				
					
	//===================================================================================
	//-------------------- FRAME METHODS ------------------------------------------------
	//===================================================================================
					
					
	public TestAppFrame(TestAppModel cacheModel) {
        this.cacheModel = cacheModel;
		try {
            viewInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
		updateCacheView();
    }


	/**
	 * Initializes frame components
	 */
    private void viewInit() throws Exception {
    	
    	initSettingsTable();
    	
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(splitPaneMain, BorderLayout.CENTER);
		
		splitPaneMain.setLeftComponent(panelLeftPart);
		splitPaneMain.setRightComponent(splitPaneCacheControl);
		
		splitPaneMain.setLastDividerLocation(300);
		splitPaneMain.setDividerLocation(300);
		
		panelLeftPart.setLayout(new BorderLayout());
		panelLeftPart.add(scrollPaneTableSettings, BorderLayout.CENTER);
		panelLeftPart.add(panelCacheManage, BorderLayout.SOUTH);
		
		scrollPaneTableSettings.setViewportView(tableSettings);
		scrollPaneTableSettings.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPaneTableSettings.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		
		panelCacheManage.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panelCacheManage.setLayout(new GridLayout(3, 2, 8, 8));
		panelCacheManage.add(buttonLoadSettings);
		panelCacheManage.add(buttonSaveSettings);
		panelCacheManage.add(buttonInitCache);
		panelCacheManage.add(buttonStartCache);
		panelCacheManage.add(buttonStopCache);
		panelCacheManage.add(buttonShutdownCache);
		
		
		buttonLoadSettings .setPreferredSize(new Dimension(140, 24));
		buttonSaveSettings .setPreferredSize(new Dimension(140, 24));
		buttonInitCache    .setPreferredSize(new Dimension(140, 24));
		buttonStartCache   .setPreferredSize(new Dimension(140, 24));
		buttonStopCache    .setPreferredSize(new Dimension(140, 24));
		buttonShutdownCache.setPreferredSize(new Dimension(140, 24));
		
		buttonLoadSettings .setMinimumSize(new Dimension(140, 24));
		buttonSaveSettings .setMinimumSize(new Dimension(140, 24));
		buttonInitCache    .setMinimumSize(new Dimension(140, 24));
		buttonStartCache   .setMinimumSize(new Dimension(140, 24));
		buttonStopCache    .setMinimumSize(new Dimension(140, 24));
		buttonShutdownCache.setMinimumSize(new Dimension(140, 24));
		
		buttonLoadSettings .setMaximumSize(new Dimension(140, 24));
		buttonSaveSettings .setMaximumSize(new Dimension(140, 24));
		buttonInitCache    .setMaximumSize(new Dimension(140, 24));
		buttonStartCache   .setMaximumSize(new Dimension(140, 24));
		buttonStopCache    .setMaximumSize(new Dimension(140, 24));
		buttonShutdownCache.setMaximumSize(new Dimension(140, 24));
		
		fillAlignmentX(panelCacheManage);
        
		
		
		//-------------- CACHE CONTROL ---------------------------------------------------
		
		splitPaneCacheControl.setTopComponent(panelCacheContent);
		splitPaneCacheControl.setBottomComponent(panelCacheLog);
		splitPaneCacheControl.setResizeWeight(1);
		
		panelCacheContent.setLayout(new BoxLayout(panelCacheContent, BoxLayout.Y_AXIS));
		panelCacheContent.add(panelCacheState);
		panelCacheContent.add(panelCacheEdit);
		panelCacheContent.add(panelCacheView);
		
		panelCacheState.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
		panelCacheEdit.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panelCacheView.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		
		panelCacheState.setLayout(new BorderLayout());
		panelCacheState.add(labelCacheState, BorderLayout.CENTER);
		panelCacheState.add(buttonUpdateView, BorderLayout.EAST);
		
		panelCacheEdit.setLayout(new BoxLayout(panelCacheEdit, BoxLayout.Y_AXIS));
		panelCacheEdit.add(panelKeys);
		panelCacheEdit.add(panelValues);
		panelCacheEdit.add(panelEditButtons);
		
		panelKeys.setLayout(new BorderLayout());
		panelKeys.add(labelKey, BorderLayout.WEST);
		panelKeys.add(panelKeyFields, BorderLayout.CENTER);
		panelKeys.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		
		panelKeyFields.setLayout(new GridLayout(1, 3, 5, 12));
		panelKeyFields.add(fieldKeyPut);
		panelKeyFields.add(fieldKeyGet);
		panelKeyFields.add(fieldKeyRemove);
		
		panelValues.setMinimumSize(new Dimension(0, 50));
		panelValues.setPreferredSize(new Dimension(0, 50));
		panelValues.setSize(0, 50);
		panelValues.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		panelValues.setLayout(new BorderLayout());
		panelValues.add(labelValue, BorderLayout.WEST);
		panelValues.add(panelValueFields, BorderLayout.CENTER);
		
		panelValueFields.setLayout(new GridLayout(1, 3, 5, 12));
		panelValueFields.add(scrollPaneValuePut);
		panelValueFields.add(scrollPaneValueGet);
		panelValueFields.add(scrollPaneValueRemove);
		
		scrollPaneValuePut.setViewportView(textAreaValuePut);
		scrollPaneValueGet.setViewportView(textAreaValueGet);
		scrollPaneValueRemove.setViewportView(textAreaValueRemove);
		
		textAreaValuePut.setFont(fieldKeyPut.getFont());
		textAreaValueGet.setFont(fieldKeyGet.getFont());
		textAreaValueRemove.setFont(fieldKeyRemove.getFont());
		
		panelEditButtons.setLayout(new BorderLayout());
		panelEditButtons.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
		panelEditButtons.add(labelButtons, BorderLayout.WEST);
		panelEditButtons.add(panelEditButtonsGrid, BorderLayout.CENTER);
		
		panelEditButtonsGrid.setLayout(new GridLayout(1, 3, 5, 12));
		
		panelEditButtonsGrid.add(buttonPut);
		panelEditButtonsGrid.add(buttonGet);
		panelEditButtonsGrid.add(buttonRemove);
		
		labelKey.setMinimumSize(new Dimension(45, 20));
		labelValue.setMinimumSize(new Dimension(45, 20));
		labelButtons.setMinimumSize(new Dimension(45, 20));
		
		labelKey.setPreferredSize(new Dimension(45, 20));
		labelValue.setPreferredSize(new Dimension(45, 20));
		labelButtons.setPreferredSize(new Dimension(45, 20));
		
		labelValue.setVerticalAlignment(JLabel.TOP);
		labelValue.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		
		panelCacheView.setLayout(new GridLayout(1, 2, 5, 12));
		panelCacheView.add(panelMemoryCacheContent);
		panelCacheView.add(panelFsCacheContent);
		
		panelMemoryCacheContent.setLayout(new BorderLayout());
		panelMemoryCacheContent.add(labelMemoryCache, BorderLayout.NORTH);
		panelMemoryCacheContent.add(scrollPaneMemoryCache, BorderLayout.CENTER);
		
		scrollPaneMemoryCache.setViewportView(tableMemoryCache);
		tableMemoryCache.setModel(new CacheViewTableModel());
		tableMemoryCache.getColumnModel().getColumn(0).setHeaderValue("Key");
		tableMemoryCache.getColumnModel().getColumn(1).setHeaderValue("Info");
		
		panelFsCacheContent.setLayout(new BorderLayout());
		panelFsCacheContent.add(labelFsCache, BorderLayout.NORTH);
		panelFsCacheContent.add(scrollPaneFsCache, BorderLayout.CENTER);
		
		scrollPaneFsCache.setViewportView(tableFsCache);
		tableFsCache.setModel(new CacheViewTableModel());
		tableFsCache.getColumnModel().getColumn(0).setHeaderValue("Key");
		tableFsCache.getColumnModel().getColumn(1).setHeaderValue("Info");
		
		panelCacheLog.setLayout(new BorderLayout());
		panelCacheLog.setPreferredSize(new Dimension(Integer.MAX_VALUE, 150));
		panelCacheLog.setMinimumSize(new Dimension(0, 150));
		panelCacheLog.add(scrollPaneCacheLog);
		scrollPaneCacheLog.setViewportView(textAreaCacheLog);
		
		textAreaCacheLog.setFont(new Font("Courier New", Font.PLAIN, 12));
    }
	
    
    /**
     * Sets left alignment for all child components of given container
     */
    void fillAlignmentX(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent)c).setAlignmentX(JComponent.LEFT_ALIGNMENT);
            }
        }
    }
    
    
    /**
     * Initializes configuration settings table
     */
    void initSettingsTable() throws IOException {
    	
		tableSettings.setModel(cacheModel.getSettingsTableModel());
		
		tableSettings.getColumnModel().getColumn(0).setHeaderValue("Cache Property");
		tableSettings.getColumnModel().getColumn(1).setHeaderValue("Property Value");
		
		tableSettings.getColumnModel().getColumn(0).setMaxWidth(120);
		tableSettings.getColumnModel().getColumn(0).setMinWidth(120);
		tableSettings.getColumnModel().getColumn(0).setWidth(120);
		
		tableSettings.getColumnModel().getColumn(1).setMaxWidth(Integer.MAX_VALUE);
		tableSettings.getColumnModel().getColumn(1).setMinWidth(120);
		tableSettings.getColumnModel().getColumn(1).setWidth(120);
		
		tableSettings.setRowHeight(20);
    }
    
    
    /**
     * Updates all components view that represent cache state and data
     */
    void updateCacheView() {
		
    	TlvCacheInstance cache = cacheModel.getCacheInstance();
		TlvCacheState cacheState = (cache == null ? null : cache.getState());
		
		buttonInitCache.setEnabled(cacheState == null || cacheState == TlvCacheState.CREATED || cacheState == TlvCacheState.STOPPED);
		buttonStartCache.setEnabled(cacheState == TlvCacheState.CREATED);
		buttonStopCache.setEnabled(cacheState == TlvCacheState.STARTING || cacheState == TlvCacheState.WORKING);
		buttonShutdownCache.setEnabled(cacheState == TlvCacheState.STARTING || cacheState == TlvCacheState.WORKING || cacheState == TlvCacheState.STOPPING);
		
		buttonPut.setEnabled(cacheState == TlvCacheState.WORKING);
		buttonGet.setEnabled(cacheState == TlvCacheState.WORKING);
		buttonRemove.setEnabled(cacheState == TlvCacheState.WORKING);
		
		labelCacheState.setText(cacheModel.getCacheStateDesciption());
		if (cache != null) {
			( (CacheViewTableModel) tableMemoryCache.getModel()).setData(cache.getMemoryCacheContentSnapshot());
			( (CacheViewTableModel) tableFsCache.getModel()).setData(cache.getFsCacheContentSnapshot());
		}
	}
}
