package com.tlvcache.testapp;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * Test application main class
 */
public class TestApplication {

	
	/**
	 * Initialize base logger for all subpackeges
	 */
	static final Logger LOGGER = Logger.getLogger("com.wiley.cache");
	static {
		LOGGER.setLevel(Level.ALL);
	}

	
	
	public TestApplication() {

		final TestAppModel model = new TestAppModel();
		final TestAppFrame frame = new TestAppFrame(model);
		TestAppController.create(model, frame);

		frame.setTitle("Two level cache - Testing Application");

		
		//--- set frame size
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width <= 800) {
			frame.setSize(new Dimension(780, 500));
		} else {
			frame.setSize(new Dimension(1000, 600));
		}

		
		//--- place frame at the center of screen
		
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2,
				(screenSize.height - frameSize.height) / 2);

		
		//--- initialize window close handler
		
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			/**
			 * Shutdowns cache and terminate program on main frame closed
			 */
			@Override
			public void windowClosing(WindowEvent e) {
				model.shutdownCache();
				System.exit(0);
			}
		});

		
		//--- show main frame
		frame.setVisible(true);
	}

	
	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		new TestApplication();
	}
}
