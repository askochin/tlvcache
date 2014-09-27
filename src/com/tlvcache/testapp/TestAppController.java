package com.tlvcache.testapp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.tlvcache.TlvCacheException;

/**
 * Main controller of application 
 * (as part of MVC pattern)
 */
public class TestAppController {

	private final Logger logger = Logger.getLogger(TestAppController.class.getName());
	
	private final TestAppModel model;
	private final TestAppFrame appFrame;
	
	
	private TestAppController(TestAppModel cacheModel, TestAppFrame appFrame) {
		this.model = cacheModel;
		this.appFrame = appFrame;
	}
	
	
	static TestAppController create(TestAppModel cacheModel, TestAppFrame appFrame) {
		TestAppController controller = new TestAppController(cacheModel, appFrame);
		controller.init();
		return controller;
	}


	private void init() {
		
		//-------- Create buttons listeners ---------------------------------
		
		appFrame.buttonLoadSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				loadConfig();
			}
		});
		
		
		appFrame.buttonSaveSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				saveConfig();
			}
		});
		
		
		appFrame.buttonInitCache.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doInit();
			}
		});
		
		appFrame.buttonStartCache.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doStart();
			}
		});
		
		appFrame.buttonStopCache.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doStop();
			}
		});
		
		appFrame.buttonShutdownCache.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doShutdown();
			}
		});
		
		appFrame.buttonUpdateView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				appFrame.updateCacheView();
			}
		});
		
		appFrame.buttonGet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doGet();
			}
		});
		
		appFrame.buttonPut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doPut();
			}
		});
		
		appFrame.buttonRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				doRemove();
			}
		});
		
		//-------- Create handler to pass log messages to the text area ---

		Handler fh = new TextAreaHandler();
		fh.setLevel(Level.ALL);
		TestApplication.LOGGER.addHandler(fh);
		TestApplication.LOGGER.setLevel(Level.FINE);
	}
	
	
	/**
	 * Shows file chooser dialog and load configuration params 
	 * from selected file
	 */
	void loadConfig() {
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Cache configuration file");
        fileChooser.setApproveButtonText("Open");
        int result = fileChooser.showOpenDialog(appFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            Properties props = new Properties();
            try (FileReader f = new FileReader(fileChooser.getSelectedFile().getAbsolutePath())) {
            	props.load(f);
            	model.initCacheSettings(props);
            	model.getSettingsTableModel().fireTableDataChanged();
            } catch (IOException ex) {
            	JOptionPane.showMessageDialog(appFrame, "Ошибка при загрузке конфигурации: " + ex.getMessage(),
					"Загрузка конфигурации кэша", JOptionPane.WARNING_MESSAGE);
            }
        }
	}
	
	
	/**
	 * Opens file chooser dialog and save configuration params 
	 * to selected file
	 */
	void saveConfig() {
		JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Cache configuration");
        fileChooser.setApproveButtonText("Save");
        int result = fileChooser.showOpenDialog(appFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream f = new FileOutputStream(fileChooser.getSelectedFile().getAbsolutePath())) {
            	model.getSettingsTableModel().getAsProperties().store(f, "");
            } catch (IOException ex) {
            	JOptionPane.showMessageDialog(appFrame, "Ошибка при сохранении конфигурации: " + ex.getMessage(),
					"Сохранение конфигурации кэша", JOptionPane.WARNING_MESSAGE);
            }
        }
	}
	
	
	/**
	 * Starts cache instance
	 */
	void doStart() {
		if (model.getCacheInstance() == null) return;
		new Thread() {
			
			@Override
			public void run() {
				invokeUpdateCacheView();
				try {
					model.getCacheInstance().start();
				} catch (final TlvCacheException e) {
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(appFrame, "Cannot start cache: " + e.getMessage(),
								"Start cache", JOptionPane.WARNING_MESSAGE);
						}
					});
					
					
					logger.log(Level.WARNING, "Cannot start cache: " + e.getMessage(), e);
				} finally {
					invokeUpdateCacheView();
				}
			}
		}.start();
	}
	
	
	/**
	 * Stops cache instance
	 */
	void doStop() {
		if (model.getCacheInstance() == null) return;
		model.getCacheInstance().stop(new Runnable() {
			@Override
			public void run() {
				invokeUpdateCacheView();
			}
		});
	}
	
	
	/**
	 * Shutdown cache instance
	 */
	void doShutdown() {
		if (model.getCacheInstance() == null) return;
		new Thread() {
			
			@Override
			public void run() {
				try {
					if (model.getCacheInstance().shutdown(10, TimeUnit.SECONDS)) {
						invokeUpdateCacheView();
					}
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, "Shutdown cache thread interrupted: " + e.getMessage(), e);
					Thread.currentThread().interrupt();
				}
			}
		}.start();
		
	}
	
	
	/**
	 * Create and initialize new cache instance using current configuration params
	 */
	void doInit() {
		try {
			model.initNewCacheInstance();
			appFrame.updateCacheView();
		} catch (TlvCacheException ex) {
			JOptionPane.showMessageDialog(null, "Cannot initialize new cache instance: " + ex.getMessage(), 
				"Init cache", JOptionPane.WARNING_MESSAGE);
			logger.log(Level.WARNING, "Cannot init new cache instance", ex);
			
		}
	}
	
	
	/**
	 * Put value to cache
	 */
	void doPut() {
		
		String key = appFrame.fieldKeyPut.getText();
		if (key.trim().equals("")) {
			JOptionPane.showMessageDialog(appFrame, "Cannot put value: Empty key.", 
				"Put value", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		String value = appFrame.textAreaValuePut.getText();
		if (value.trim().equals("")) {
			JOptionPane.showMessageDialog(appFrame, "Cannot put value: Empty value.",
				"Put value", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		model.getCacheInstance().put(key, value);
		appFrame.updateCacheView();
	}
	
	
	/**
	 * Retrieves and show value from cache
	 */
	void doGet() {
		String key = appFrame.fieldKeyGet.getText();
		if (key.trim().equals("")) {
			JOptionPane.showMessageDialog(appFrame, "Cannot get value: Empty key.",
				"Get value", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Object value = model.getCacheInstance().get(key);
		appFrame.textAreaValueGet.setText(value == null ? "null" : value.toString());
	}
	
	
	/**
	 * Removes value from cache by key
	 */
	void doRemove() {
		String key = appFrame.fieldKeyRemove.getText();
		if (key.trim().equals("")) {
			JOptionPane.showMessageDialog(appFrame, "Cannot remove value: Empty key.",
				"Remove value", JOptionPane.WARNING_MESSAGE);
			return;
		}
		model.getCacheInstance().remove(key);
		Object value = model.getCacheInstance().get(key);
		appFrame.textAreaValueRemove.setText(value == null ? "null" : value.toString());
		appFrame.updateCacheView();
	}
	
	
	/**
	 * Add update frame task to Swing event queue 
	 */
	private void invokeUpdateCacheView() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				appFrame.updateCacheView();
			}
		});
	}
	
	
	/**
	 * Log handler to write log messages to text area
	 */
	class TextAreaHandler extends java.util.logging.Handler {

		@Override
		public void publish(final LogRecord record) {
			
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					
					StringWriter text = new StringWriter();
					PrintWriter out = new PrintWriter(text);
					out.println(appFrame.textAreaCacheLog.getText());
					
					String loggerName = record.getLoggerName();
					int dotIndex = loggerName.lastIndexOf('.');
					if (dotIndex >= 0) {
						loggerName = loggerName.substring(dotIndex + 1, loggerName.length());
					}
					
					out.printf("[%s] [Thread-%d] %s : %s",
						record.getLevel(), record.getThreadID(),
						loggerName, record.getMessage());
					
					appFrame.textAreaCacheLog.setText(text.toString());
				}

			});
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}

	}
}
