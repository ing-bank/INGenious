
package com.ing.ide.util;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ing.util.encryption.Encryption;

public class Utility {

	public static final FileFilter DIR_FILTER = File::isDirectory;
	static Path path;
	static BasicFileAttributes attr;
	static File file;
	public static FileNameExtensionFilter csvFIlter = new FileNameExtensionFilter("CSV File", "csv"),
			exeFilter = new FileNameExtensionFilter("Executable Files", "exe"),
			dbFilter = new FileNameExtensionFilter("SqLite Database", "db");
	private static final Icon C_SAVE = new ImageIcon(Utility.class.getResource("/ui/resources/csave.png"));

	private Utility() {

	}

	public static boolean confirmSave(JComponent parent, String val) {
		int op = JOptionPane.showConfirmDialog(parent, "Do you want to save the " + val + " ?", "Save",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, C_SAVE);
		return op == JOptionPane.YES_OPTION;
	}

	public static boolean isEmpty(Object val) {
		return val == null || "".equals(val.toString().trim());
	}

	public static String getdatetimeString() {
		Date dat = new Date();
		return getDATE_FILE_FORMAT().format(dat) + "_" + getTIME_FILE_FORMAT().format(dat);
	}

	public static String getdateString() {
		Date dat = new Date();
		return getDATE_FILE_FORMAT().format(dat);
	}

	/**
	 * @return the DATA_FORMAT
	 */
	public static SimpleDateFormat getDATA_FORMAT() {
		return new SimpleDateFormat("MM/dd/yyyy");
	}

	/**
	 * @return the TIME_FORMAT
	 */
	public static SimpleDateFormat getTIME_FORMAT() {
		return new SimpleDateFormat("hh:mm:ss a");
	}

	/**
	 * @return the DATE_FILE_FORMAT
	 */
	public static SimpleDateFormat getDATE_FILE_FORMAT() {
		return new SimpleDateFormat("MM-dd-yyyy");
	}

	/**
	 * @return the TIME_FILE_FORMAT
	 */
	public static SimpleDateFormat getTIME_FILE_FORMAT() {
		return new SimpleDateFormat("hh-mm-ssa");
	}

	/**
	 * @return the LIC_DATE_FORMAT
	 */
	public static SimpleDateFormat getLIC_DATE_FORMAT() {
		return new SimpleDateFormat("ddMMyyyy");
	}

	public static void enableComponent(Container con, boolean state, Component component) {
		for (Component c : con.getComponents()) {
			if (c instanceof Container) {
				enableComponent((Container) c, state, component);
			}
			if (component != null && component.equals(c)) {
				continue;
			}
			if (c instanceof JLabel) {
				c.setVisible(state);
			} else {
				c.setEnabled(state);
			}
		}
	}

	public static String getMessageFromJOptionPane(String message, String title) {
		return JOptionPane.showInputDialog(null, message, title, JOptionPane.QUESTION_MESSAGE);
	}

	public static String getValue(Object value) {
		if (isEmpty(value)) {
			return "";
		}
		return value.toString();
	}

	/**
	 * return the Date difference in no of days
	 *
	 * @param exp
	 * @return
	 */
	public static int getDays(Date exp) {
		try {
			Date today = new Date();
			if (exp != null) {
				long diff = exp.getTime() - today.getTime();
				return (int) (diff / (24 * 60 * 60 * 1000));
			}
		} catch (Exception ex) {
			Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
		}
		return 30;
	}

	public static Properties loadProperties(String location) {
		Properties prop = new Properties();
		if (new File(location).exists()) {
			try (FileInputStream inputStream = new FileInputStream(location)) {
				prop.load(inputStream);
			} catch (IOException ex) {
				Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return prop;
	}

	public static String encrypt(String data) {
		if (data != null && !data.trim().isEmpty()) {
			if (data.endsWith(" Enc")) {
				return data;
			} else {
				data = Encryption.getInstance().encrypt(data);
				return data + " Enc";
			}
		}
		return data;
	}
}
