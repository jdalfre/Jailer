/*
 * Copyright 2007 - 2022 Ralf Wisser.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jailer.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.fife.rsta.ui.EscapableDialog;

import net.sf.jailer.ExecutionContext;
import net.sf.jailer.JailerVersion;
import net.sf.jailer.datamodel.DataModel;
import net.sf.jailer.modelbuilder.JDBCMetaDataBasedModelElementFinder;
import net.sf.jailer.modelbuilder.ModelBuilder;
import net.sf.jailer.ui.DbConnectionDialog.ConnectionInfo;
import net.sf.jailer.ui.UIUtil.PLAF;
import net.sf.jailer.ui.commandline.CommandLineInstance;
import net.sf.jailer.ui.databrowser.BookmarksPanel;
import net.sf.jailer.ui.databrowser.BookmarksPanel.BookmarkId;
import net.sf.jailer.ui.databrowser.DataBrowser;
import net.sf.jailer.ui.util.UISettings;
import net.sf.jailer.util.Pair;

/**
 * Data Model Management dialog.
 *
 * @author Ralf Wisser
 */
public abstract class DataModelManagerDialog extends javax.swing.JFrame {

	/**
	 * <code>true</code> if a model is selected.
	 */
	public boolean hasSelectedModel = false;

	/**
	 * List of available models.
	 */
	private List<String> modelList;

	/**
	 * Model details as pair of folder-name and last-modified timestamp.
	 */
	private Map<String, Pair<String, Long>> modelDetails;

	/**
	 * Currently selected model.
	 */
	private String currentModel;

	/**
	 * Model base folders.
	 */
	private List<String> baseFolders = new ArrayList<String>();

	private final ExecutionContext executionContext = new ExecutionContext();

	private DbConnectionDialog dbConnectionDialog;
	private DbConnectionDialog recUsedConnectionDialog;
	private InfoBar infoBarConnection;
	private InfoBar infoBarRecUsedConnection;
	private InfoBar infoBarBookmark;
	private InfoBar infoBarRecUsedBookmark;
	private final String tabPropertyName;
	private final String module; // TODO define module enum

	private Font font =  new JLabel("normal").getFont();
	private Font normal = new Font(font.getName(), font.getStyle() & ~Font.BOLD, font.getSize());
    private Font bold = new Font(font.getName(), font.getStyle() | Font.BOLD, font.getSize());

	/**
	 * Creates new.
	 */
	public DataModelManagerDialog(String applicationName, boolean withLoadJMButton, String module) {
		this.applicationName = applicationName;
		this.tabPropertyName = "DMMDPropTab" + module;
		this.module = module;
		initComponents();
		((CardLayout) cardPanel.getLayout()).show(cardPanel, "main");
		
		histLabel.setIcon(UIUtil.scaleIcon(histLabel, histIcon));
		
		InfoBar infoBar = new DMMDInfoBar("Data Model Configuration",
				"A data model is a set of interrelated tables. Acquire information about tables by analyzing\n" +
				"database schemas, or use the data model editor to manually define tables and associations.\n \n",
				"Select a data model to work with.");
		UIUtil.replace(infoBarLabel, infoBar);

		InfoBar infoBarJM = new DMMDInfoBar("Load Extraction Model",
				"\n \n \n \n",
				"Load a recently used model or choose a model file.");
		UIUtil.replace(infoBarLabel2, infoBarJM);

		infoBarConnection = new DMMDInfoBar("Database Connection",
				"Select a database connection. Please note that here only existing connections can be selected.\n" +
				"To create new connections, first select the data model in the \"Data Model\" tab to which this will belong.\n \n",
				"Select a database to work with.");

		infoBarRecUsedConnection = new DMMDInfoBar("Recently used Database Connection",
				"Select a recently used connection to the database.\n" +
				"\n \n \n",
				"Select a database to work with.");

		infoBarBookmark = new DMMDInfoBar("Bookmark",
				"Select a bookmark to open.\n" +
				"\n \n \n",
				"Select a bookmark.");
		UIUtil.replace(infoBarLabelBookmark, infoBarBookmark);

		infoBarRecUsedBookmark = new DMMDInfoBar("Recently used Bookmark",
				"Select a recently used bookmark to open.\n" +
				"\n \n \n",
				"Select a bookmark.");
		UIUtil.replace(infoBarLabeRecUsedlBookmark, infoBarRecUsedBookmark);

		restoreButton.setIcon(UIUtil.scaleIcon(restoreButton, histIcon));
		okButton.setIcon(UIUtil.scaleIcon(okButton, okIcon));
		jButton2.setIcon(UIUtil.scaleIcon(jButton2, cancelIcon));
		bmOkButton.setIcon(UIUtil.scaleIcon(bmOkButton, okIcon));
		bmCancelButton.setIcon(UIUtil.scaleIcon(bmCancelButton, cancelIcon));
		
		if (!withLoadJMButton) {
			jTabbedPane1.remove(loadJMPanel);
		} else {
			jTabbedPane1.remove(bookmarkPanel);
			jTabbedPane1.remove(recentlyUsedBookmarkPanel);
		}

		if (module.equals("S")) {
			jTabbedPane1.remove(recUsedConnectionPanel);
			jTabbedPane1.remove(loadJMPanel);
		} else {
			jTabbedPane1.remove(recUsedConnectionPanel);
			jTabbedPane1.remove(recentlyUsedBookmarkPanel);
		}
		
		try {
			ImageIcon imageIcon = UIUtil.readImage("/jailer.png");
			setIconImage(imageIcon.getImage());
		} catch (Throwable t) {
		}
		
		GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel4.add(locationComboBox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(2, 0, 2, 0);
        jPanel10.add(recentSessionsComboBox, gridBagConstraints);
        recentSessionsComboBox.setMaximumRowCount(12);
		ImageIcon imageIcon = UIUtil.jailerLogo;
		infoBar.setIcon(imageIcon);
		infoBarJM.setIcon(imageIcon);
		infoBarConnection.setIcon(imageIcon);
		infoBarRecUsedConnection.setIcon(imageIcon);
		infoBarBookmark.setIcon(imageIcon);
		infoBarRecUsedBookmark.setIcon(imageIcon);

		restore();

		loadModelList();
		initTableModel();
		initConnectionDialog(true);
		initConnectionDialog(false);
		JTable bookmarkTable = initBookmarkTables();
		bookmarkTable.setAutoCreateRowSorter(true);
		
		final TableCellRenderer defaultTableCellRenderer = dataModelsTable
				.getDefaultRenderer(String.class);
		dataModelsTable.setShowGrid(false);
		dataModelsTable.setDefaultRenderer(Object.class,
				new TableCellRenderer() {

					@Override
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Component render = defaultTableCellRenderer
								.getTableCellRendererComponent(table, value,
										false, false /* hasFocus */, row, column);
						if (render instanceof JLabel) {
							final Color BG1 = UIUtil.TABLE_BACKGROUND_COLOR_1;
							final Color BG2 = UIUtil.TABLE_BACKGROUND_COLOR_2;
							if (!isSelected) {
								((JLabel) render)
									.setBackground((row % 2 == 0) ? BG1
											: BG2);
							} else {
								((JLabel) render).setBackground(new Color(160, 200, 255));
							}
						}
						render.setFont(column == 0? bold : normal);
						return render;
					}
				});
		dataModelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dataModelsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent evt) {
						if (dataModelsTable.getSelectedRow() >= 0) {
							currentModel = modelList.get(dataModelsTable.getSelectedRow());
						} else {
							currentModel = null;
						}
						refreshButtons();
					}
				});
		dataModelsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				JTable table =(JTable) me.getSource();
				Point p = me.getPoint();
				int row = table.rowAtPoint(p);
				if (me.getClickCount() >= 2) {
					table.getSelectionModel().setSelectionInterval(row, row);
					if (row >= 0) {
						currentModel = modelList.get(row);
					} else {
						currentModel = null;
					}
					refreshButtons();
					if (currentModel != null) {
						okButtonActionPerformed(null);
					}
				}
			}
		});
		
		try {
			((DefaultTableCellRenderer) dataModelsTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
		} catch (Exception e) {
			// ignore
		}

		initJMTable();

		updateLocationComboboxModel();
		locationComboBox.setSelectedItem(executionContext.getDatamodelFolder());
		final ListCellRenderer<String> renderer = locationComboBox.getRenderer();
		locationComboBox.setRenderer(new ListCellRenderer<String>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
					boolean isSelected, boolean cellHasFocus) {
				try {
					value = new File(value).getAbsolutePath();
				} catch (Exception e) {
					// ignore
				}
				return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});
		locationComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getItem() != null) {
					executionContext.setDatamodelFolder((String) e.getItem());
					loadModelList();
					refresh();
				}
			}
		});

		if (currentModel != null) {
			int i = modelList.indexOf(currentModel);
			if (i >= 0) {
				dataModelsTable.getSelectionModel().setSelectionInterval(i, i);
			} else {
				currentModel = null;
			}
		}

		setTitle(applicationName);

		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				UIUtil.invokeLater(() -> restoreButton.grabFocus());
				openWelcomeDilog();
			}
			@Override
			public void windowIconified(WindowEvent e) {
			}
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			@Override
			public void windowClosing(WindowEvent e) {
			}
			@Override
			public void windowClosed(WindowEvent e) {
		        UIUtil.invokeLater(new Runnable() {
					@Override
					public void run() {
						UIUtil.checkTermination();
					}
				});
			}
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});

		setLocation(70, 80);
		pack();
		setSize(Math.max(840, getWidth()), 536);
		UIUtil.fit(this);
		refresh();

		initRestoreLastSessionButton();

		if (jTabbedPane1.getSelectedComponent() == recentlyUsedBookmarkPanel) {
			if (bookmarkTable.getModel().getRowCount() > 0) {
				bookmarkTable.getSelectionModel().setSelectionInterval(0, 0);
			}
			if (bmRecUsedOkButton.isEnabled()) {
				bmRecUsedOkButton.grabFocus();
			}
		} else if (jTabbedPane1.getSelectedComponent() == loadJMPanel) {
			if (jmFilesTable.getModel().getRowCount() > 0) {
				jmFilesTable.getSelectionModel().setSelectionInterval(0, 0);
			}
			if (jmOkButton.isEnabled()) {
				jmOkButton.grabFocus();
			}
		} else if (jTabbedPane1.getSelectedComponent() == recUsedConnectionPanel) {
			if (recUsedConnectionDialog != null) {
				recUsedConnectionDialog.selectFirstConnection();
			}
		} else {
			if (okButton.isEnabled()) {
				okButton.grabFocus();
			}
		}
	}

	private static boolean wdOpened = false;
	
	protected void openWelcomeDilog() {
		if (wdOpened) {
			return;
		}
		wdOpened = true;

		if (!"B".equals(module)) {
			return;
		}

		final String ASK_LATER = "WelcomeAskLater"; 
		Object askLater = UISettings.restore(ASK_LATER);
		if (Boolean.FALSE.equals(askLater)) {
			return;
		}
		if (askLater == null) {
			if (UISettings.restore("uuid") != null) {
				if (UISettings.restore("buser") == null) {
					return;
				}
			}
		}
		UISettings.store(ASK_LATER, false);
		
		ciOfBookmark.entrySet().stream().filter(e -> "INVENTORY - ACTOR".equals(e.getKey().bookmark)).findAny()
				.ifPresent(e -> {
					WelcomePanel welcomePanel = new WelcomePanel();
					
					DMMDInfoBar iBar = new DMMDInfoBar("Welcome",
							"Since you are here for the first time, \nI suggest showing you the demo database.    \n",
							"");
					iBar.setIcon(UIUtil.jailerLogo);
					UIUtil.replace(welcomePanel.infoLabel, iBar);

					EscapableDialog dialog = new EscapableDialog(this) {
					};
					
					dialog.setTitle("Jailer " + JailerVersion.VERSION);
					
					welcomePanel.okButton.addActionListener(evt -> {
						BookmarkId bookmark = e.getKey();
						ConnectionInfo ci = e.getValue();
						UIUtil.subModule += 1;
						UIUtil.setWaitCursor(dialog);
						openBookmark(new BookmarkId(bookmark.bookmark, bookmark.datamodelFolder, bookmark.connectionAlias, bookmark.rawSchemaMapping), ci);
						UIUtil.resetWaitCursor(dialog);
						dialog.setVisible(false);
						dialog.dispose();
						setVisible(false);
						dispose();
					});
					welcomePanel.notYetButton.addActionListener(evt -> {
						UISettings.store(ASK_LATER, true);
						dialog.dispose();
						dialog.setVisible(false);
					});
					welcomePanel.noButton.addActionListener(evt -> {
						dialog.dispose();
						dialog.setVisible(false);
					});
					
					dialog.getContentPane().add(welcomePanel);
					dialog.pack();
					Point ownerLoc;
					Dimension ownerSize;
					ownerLoc = getLocation();
					ownerSize = getSize();
					int x = (int) (ownerLoc.getX() + ownerSize.getWidth() / 2 - dialog.getWidth() / 2);
					int y = (int) (ownerLoc.getY() + ownerSize.getHeight() / 2 - dialog.getHeight() / 2);
					dialog.setLocation(x, y);
					dialog.setVisible(true);
				});
	}

	private JTable initBookmarkTables() {
		loadBookmarks();
		JTable bookmarkTable = createBookmarkTable(bmOkButton, false);
		bookmarkDialogPanel.removeAll();
		bookmarkDialogPanel.add(new JScrollPane(bookmarkTable));
		bookmarkTable = createBookmarkTable(bmRecUsedOkButton, true);
		bookmarkRecUsedDialogPanel.removeAll();
		bookmarkRecUsedDialogPanel.add(new JScrollPane(bookmarkTable));
		return bookmarkTable;
	}

	@SuppressWarnings("unchecked")
	private void initRestoreLastSessionButton() {
		restoreButton.setVisible(true);
		recentSessionsComboBox.setVisible(true);
		dummyLabel.setVisible(false);
		final boolean forEMEditor = "S".equals(module);
		final List<BookmarkId> lastSessions = UISettings.restoreLastSessions(module);
		if (lastSessions == null || lastSessions.isEmpty()) {
			restoreButton.setEnabled(false);
			hideRecentSessionsPanel();
			dummyLabel.setVisible(true);
			return;
		}
		
		List<String> model = new ArrayList<String>();
		List<ImageIcon> logos = new ArrayList<ImageIcon>();
		List<ActionListener> actions = new ArrayList<ActionListener>();
		
		for (Iterator<BookmarkId> i = lastSessions.iterator(); i.hasNext(); ) {
			BookmarkId lastSession = i.next();
			Date date = lastSession.date;
			if (date == null || !modelList.contains(lastSession.datamodelFolder)) {
				i.remove();
				continue;
			}
			ConnectionInfo connectionInfo = null;
			for (ConnectionInfo ci: dbConnectionDialog.getConnectionList()) {
				if (ci.alias != null && ci.alias.equals(lastSession.connectionAlias)) {
					connectionInfo = ci;
					break;
				}
			}
			if (connectionInfo == null && !forEMEditor) {
				i.remove();
				continue;
			}
			Pair<String, Long> details = modelDetails.get(lastSession.datamodelFolder);
			String userName = connectionInfo != null && connectionInfo.user != null && connectionInfo.user.length() > 0? connectionInfo.user : " ";
			String dbmsLogoURL = connectionInfo != null? UIUtil.getDBMSLogoURL(connectionInfo.url) : null;
			
			if (dbmsLogoURL == null) {
				logos.add(null);
			} else {
				logos.add(UIUtil.scaleIcon(new JLabel(), UIUtil.readImage(dbmsLogoURL, false), 1.0));
			}
			
			model.add("<html><nobr>" + 
					UIUtil.toHTMLFragment(UIUtil.toDateAsString(date.getTime()), 0) + "&nbsp;-&nbsp;" +
					(module.equals("S")?
					"<font color=\"#0000ff\"><b>" +
					(lastSession.bookmark != null? UIUtil.toHTMLFragment(new File(lastSession.bookmark).getName(), 0) : "</b><i><font color=\"#888888\">New&nbsp;Model</font></i><b>") + "</b>" + 
					"&nbsp;-&nbsp;</font>" : 
					"") +
					(module.equals("S")?
					("<font color=\"#006600\">" +
					UIUtil.toHTMLFragment(((details != null? details.a : lastSession.datamodelFolder)), 0) + 
					"</font>&nbsp;-&nbsp;<font color=\"#663300\">" +
					(connectionInfo == null? "<i><font color=\"#888888\">offline</font></i>" : UIUtil.toHTMLFragment(connectionInfo.alias, 0) + "&nbsp;-&nbsp;<font color=\"#000000\">" + UIUtil.toHTMLFragment(((userName + " - ") + connectionInfo.url), 0, false) + "</font>") + 
					"</font></nobr></html>")
					:
					(
					"<font color=\"#006600\">" +
					(connectionInfo == null? "<i><font color=\"#888888\">offline</font></i>" : ("<b>" + UIUtil.toHTMLFragment(connectionInfo.alias, 0) + "</b>") + "&nbsp;-&nbsp;<font color=\"#000000\">" + 
					"<font color=\"#0000ff\">" +
					(lastSession.getContentInfo() != null? UIUtil.toHTMLFragment(lastSession.getContentInfo().replaceFirst("^\\d+ Table$", "$0s"), 0) + "&nbsp;-&nbsp;" : "") + 
					"</font>" +
					"<font color=\"#663300\">" +
					UIUtil.toHTMLFragment(((details != null? details.a : lastSession.datamodelFolder)), 0) + 
					"</font>&nbsp;-&nbsp;" + UIUtil.toHTMLFragment(userName + " - " + connectionInfo.url, 0, false) + "</font>") + 
					"</font>")					
					) +
					"</nobr></html>");
			final ConnectionInfo finalConnectionInfo = connectionInfo;
			actions.add(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					lastSessionRestored = true;
					BookmarkId bookmark = new BookmarkId(forEMEditor? lastSession.bookmark : "", lastSession.datamodelFolder, lastSession.connectionAlias, lastSession.rawSchemaMapping);
					bookmark.setContent(lastSession.getContent());
					bookmark.setContentInfo(lastSession.getContentInfo());
					openBookmark(bookmark, finalConnectionInfo);
					UIUtil.resetWaitCursor(DataModelManagerDialog.this);
					setVisible(false);
					dispose();
				}
			});
		}
		if (model.isEmpty()) {
			restoreButton.setEnabled(false);
			hideRecentSessionsPanel();
			dummyLabel.setVisible(true);
			return;
		}
		restoreButton.addActionListener(actions.get(0));
		ArrayList<String> modModel = new ArrayList<String>(model);
		if (!modModel.isEmpty()) {
			modModel.add(0, modModel.get(0) + " ");
		}
		recentSessionsComboBox.setModel(new DefaultComboBoxModel<String>(modModel.toArray(new String[0])));
		recentSessionsComboBox.setSelectedIndex(0);
		recentSessionsComboBox.addItemListener(new ItemListener() {
			boolean done = false;
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (!done) {
					done = true;
					UIUtil.setWaitCursor(DataModelManagerDialog.this);
					restoreButton.setEnabled(false);
					recentSessionsComboBox.setEnabled(false);
					UIUtil.invokeLater(() -> {
						actions.get(recentSessionsComboBox.getSelectedIndex() - 1).actionPerformed(null);
					});
				}
			}
		});
		Map<String, String[]> vals = new HashMap<String, String[]>();
		Map<String, ImageIcon> dbLogos = new HashMap<String, ImageIcon>();
		int[] prefWidth = new int[20];
		int maxSum = 0;
		for (int i = 0; i < model.size(); ++i) {
			String[] v = model.get(i).replaceFirst("^<html><nobr>", "").replaceFirst("</nobr></html>$", "").trim().split("&nbsp;-&nbsp;");
			vals.put(model.get(i), v);
			vals.put(model.get(i) + " ", v);
			dbLogos.put(model.get(i), logos.get(i));
			dbLogos.put(model.get(i) + " ", logos.get(i));
			int sum = 16;
			for (int j = 0; j < v.length; ++j) {
				int pWidth = new JLabel("<html><nobr>" + v[j] + "&nbsp;&nbsp;</nobr></html>").getPreferredSize().width;
				prefWidth[j] = Math.max(prefWidth[j], pWidth);
				prefWidth[j] = Math.min(prefWidth[j], module.equals("S")? j == 1? 200 : 140 : 160);
				sum += 16 + (j < v.length - 1? prefWidth[j] : pWidth);
			}
			maxSum = Math.max(maxSum, sum);
		}
		if (maxSum > 0) {
			recentSessionsComboBox.setPrefWidth(maxSum);
		}
		recentSessionsComboBox.setRenderer(new DefaultListCellRenderer() {
			ListCellRenderer renderer = recentSessionsComboBox.getRenderer();
    		@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
    			if (index == 0) {
    				return new JPanel(null);
    			}
    			--index;
                Component render = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String tooltip = null;
				if (render instanceof JLabel) {
					((JLabel) render).setToolTipText(tooltip = ((JLabel) render).getText().replace("&nbsp;-&nbsp;", "<br>"));
					if (isSelected) {
						((JLabel) render).setText(((JLabel) render).getText().replaceAll("<.?font[^>]*>", ""));
					}
				}
				String[] val = vals.get(value);
				if (val != null) {
					JPanel panel = new JPanel(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					for (int i = -1; i < val.length; ++i) {
						if (index < 0 && (module.equals("S") && i >= 4 || !module.equals("S") && i >= 5)) {
							break;
						}
						gbc.gridx = i + 1;
						gbc.gridy = 1;
						gbc.insets = new Insets(0, 0, 0, 8);
						JLabel label = new JLabel(i >= 0? "<html><nobr>" + val[i] + "</nobr></html>" : " ");
						if (i == 2 && !module.equals("S")) {
							label.setHorizontalAlignment(SwingConstants.RIGHT);
						}
						if (i < val.length - 1) {
							label.setPreferredSize(new Dimension(i >= 0? prefWidth[i] : 24, label.getPreferredSize().height));
							label.setMinimumSize(label.getPreferredSize());
							label.setMaximumSize(label.getPreferredSize());
						}
						if (i < 0) {
							label.setIcon(dbLogos.get(value));
						}
						panel.add(label, gbc);
					}
					
					gbc = new GridBagConstraints();
					gbc.gridx = 20;
					gbc.gridy = 1;
					gbc.weightx = 1;
					JLabel label = new JLabel("");
					panel.add(label, gbc);
					
					panel.setBackground(isSelected? UIUtil.plaf == PLAF.NIMBUS? new Color(240, 255, 255) : new Color(200, 200, 255) : null);
					panel.setToolTipText(tooltip);
					return panel;
				}
				return render;
			}
		});
		recentSessionsComboBox.setToolTipText(model.get(0).replace("&nbsp;-&nbsp;", "<br>"));
	}

	private void hideRecentSessionsPanel() {
		recentSessionsComboBox.setVisible(false);
		jPanel11.setVisible(false);
		jPanel12.setVisible(false);
		jTabbedPane1.setBorder(null);
	}

	private final List<BookmarksPanel.BookmarkId> bookmarks = new ArrayList<BookmarksPanel.BookmarkId>();
	private final Map<BookmarksPanel.BookmarkId, DbConnectionDialog.ConnectionInfo> ciOfBookmark = new HashMap<BookmarksPanel.BookmarkId, DbConnectionDialog.ConnectionInfo>();
	public static boolean lastSessionRestored = false;

	private void loadBookmarks() {
		bookmarks.clear();
		ciOfBookmark.clear();
		for (String model: modelList) {
			if (model != null) {
				for (ConnectionInfo ci: dbConnectionDialog.getConnectionList()) {
					if (ci.dataModelFolder != null && ci.dataModelFolder.equals(model)) {
						for (String bmName: BookmarksPanel.getAllBookmarks(model, executionContext)) {
							BookmarkId bm = new BookmarkId(bmName, model, ci.alias, null);
							bookmarks.add(bm);
							ciOfBookmark.put(bm, ci);
						}
					}
				}
			}
		}
	}

	private String bookmarkHash(BookmarkId bookmark) {
		return bookmark.bookmark + "\t" + bookmark.datamodelFolder + "\t" + bookmark.connectionAlias;
	}

	private JTable createBookmarkTable(final JButton okButton, boolean onlyRecentlyUsed) {
		final List<BookmarksPanel.BookmarkId> bookmarksListModel = new ArrayList<BookmarksPanel.BookmarkId>();
		Set<String> bookmarksHash = new HashSet<String>();
		for (BookmarksPanel.BookmarkId bookmark: bookmarks) {
			Pair<String, Long> details = modelDetails.get(bookmark.datamodelFolder);
			if (details != null) {
				ConnectionInfo ci = ciOfBookmark.get(bookmark);
				if (ci != null) {
					bookmarksListModel.add(bookmark);
					bookmarksHash.add(bookmarkHash(bookmark));
				}
			}
		}
		if (onlyRecentlyUsed) {
			List<BookmarksPanel.BookmarkId> newBookmarksListModel = new ArrayList<BookmarksPanel.BookmarkId>();
			for (BookmarkId bookmark: UISettings.loadRecentBookmarks()) {
				if (bookmarksHash.contains(bookmarkHash(bookmark))) {
					newBookmarksListModel.add(bookmark);
				}
			}
			bookmarksListModel.clear();
			bookmarksListModel.addAll(newBookmarksListModel);
		}
		bookmarksListModel.sort((a, b) ->  String.valueOf(a.bookmark).compareToIgnoreCase(String.valueOf(b.bookmark)));
		Object[][] data = new Object[bookmarksListModel.size()][];
		for (int i = 0; i < bookmarksListModel.size(); ++i) {
			BookmarkId bookmark = bookmarksListModel.get(i);
			Pair<String, Long> details = modelDetails.get(bookmark.datamodelFolder);
			ConnectionInfo ci = ciOfBookmark.get(bookmark);
			data[i] = onlyRecentlyUsed?
					new Object[] {
						bookmark.bookmark,
						details.a,
						bookmark.connectionAlias,
						ci.user,
						ci.url,
						UIUtil.toDateAsString(bookmark.date)
					} :
					new Object[] {
						bookmark.bookmark,
						details.a,
						bookmark.connectionAlias,
						ci.user,
						ci.url
					};
		}

		DefaultTableModel tableModel = new DefaultTableModel(data,
				onlyRecentlyUsed?
						new String[] {"Bookmark", "Data Model", "Connection", "User", "URL", "Time"}
							: new String[] { "Bookmark", "Data Model", "Connection", "User", "URL" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			private static final long serialVersionUID = 1535384744352159695L;
		};

		final JTable jTable = new JTable();
		jTable.setAutoCreateRowSorter(true);
		
		jTable.setModel(tableModel);

		final TableCellRenderer defaultTableCellRenderer = jTable.getDefaultRenderer(String.class);
		jTable.setShowGrid(false);
		jTable.setDefaultRenderer(Object.class,
			new TableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(
						JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					Component render = defaultTableCellRenderer
							.getTableCellRendererComponent(table, value,
									false, false /* hasFocus */, row, column);
					if (render instanceof JLabel) {
						final Color BG1 = UIUtil.TABLE_BACKGROUND_COLOR_1;
						final Color BG2 = UIUtil.TABLE_BACKGROUND_COLOR_2;
						if (!isSelected) {
							((JLabel) render)
								.setBackground((row % 2 == 0) ? BG1
										: BG2);
						} else {
							((JLabel) render).setBackground(new Color(160, 200, 255));
						}
						((JLabel) render).setToolTipText(((JLabel) render).getText());
					}
					render.setFont(column == 0? bold : normal);
					return render;
				}
			});
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent evt) {
					okButton.setEnabled(jTable.getSelectedRow() >= 0);
				}
			});

		final ActionListener onOk = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = jTable.getSelectedRow();
				if (i >= 0 && i < bookmarksListModel.size()) {
					BookmarkId bookmark = bookmarksListModel.get(i);
					ConnectionInfo ci = ciOfBookmark.get(bookmark);
					openBookmark(new BookmarkId(bookmark.bookmark, bookmark.datamodelFolder, bookmark.connectionAlias, bookmark.rawSchemaMapping), ci);
					setVisible(false);
					dispose();
				}
			}
		};
		okButton.addActionListener(onOk);

		jTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				JTable table =(JTable) me.getSource();
				Point p = me.getPoint();
				int row = table.rowAtPoint(p);
				if (me.getClickCount() >= 2) {
					table.getSelectionModel().setSelectionInterval(row, row);
					onOk.actionPerformed(null);
				}
			}
		});

		for (int i = 0; i < jTable.getColumnCount(); i++) {
			TableColumn column = jTable.getColumnModel().getColumn(i);
			int width = i == 0? 100 : 1;
			Component comp = jTable.getDefaultRenderer(String.class).getTableCellRendererComponent(jTable, column.getHeaderValue(), false, false, 0, i);
			width = Math.max(width, comp.getPreferredSize().width);

			for (int line = 0; line < data.length; ++line) {
				comp = jTable.getDefaultRenderer(String.class).getTableCellRendererComponent(jTable,
						data[line][i],false, false, line, i);
				width = Math.max(width, Math.min(250, comp.getPreferredSize().width));
			}
			if (onlyRecentlyUsed && i == jTable.getColumnCount() - 1) {
				width = Math.max(width, 150);
			}
			column.setPreferredWidth(width);
		}
		
		try {
			((DefaultTableCellRenderer) jTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
		} catch (Exception e) {
			// ignore
		}
		
		return jTable;
	}

	private final List<File> fileList = new ArrayList<File>();

	private void initJMTable() {
		Map<File, Date> timestamps = new HashMap<File, Date>();
		try {
			for (Pair<File, Date> file: UISettings.loadRecentFiles()) {
				if (file.a.exists()) {
					fileList.add(file.a);
				}
				timestamps.put(file.a, file.b);
			}
		} catch (Exception e) {
			// ignore
		}
		Object[][] data = new Object[fileList.size()][];
		int i = 0;
		for (File file: fileList) {
			try {
				if (file.exists()) {
					data[i++] = new Object[] { file.getName(), file.getAbsoluteFile().getParent(), UIUtil.toDateAsString(timestamps.get(file)) };
				}
			} catch (Exception e) {
				// ignore
			}
		}
		data = Arrays.copyOf(data, i);

		DefaultTableModel tableModel = new DefaultTableModel(data, new String[] { "Name", "Path", "Time" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			private static final long serialVersionUID = 1535384744352159695L;
		};
		jmFilesTable.setModel(tableModel);

		final TableCellRenderer defaultTableCellRenderer = jmFilesTable
				.getDefaultRenderer(String.class);
		jmFilesTable.setShowGrid(false);
		jmFilesTable.setDefaultRenderer(Object.class,
				new TableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Component render = defaultTableCellRenderer
								.getTableCellRendererComponent(table, value,
										false, false /* hasFocus */, row, column);
						if (render instanceof JLabel) {
							final Color BG1 = UIUtil.TABLE_BACKGROUND_COLOR_1;
							final Color BG2 = UIUtil.TABLE_BACKGROUND_COLOR_2;
							if (!isSelected) {
								((JLabel) render)
									.setBackground((row % 2 == 0) ? BG1
											: BG2);
							} else {
								((JLabel) render).setBackground(new Color(160, 200, 255));
							}
						}
						render.setFont(column == 0? bold : normal);
						return render;
					}
				});
		jmOkButton.setEnabled(false);
		jmFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jmFilesTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent evt) {
						jmOkButton.setEnabled(jmFilesTable.getSelectedRow() >= 0);
					}
				});
		for (i = 0; i < jmFilesTable.getColumnCount(); i++) {
			TableColumn column = jmFilesTable.getColumnModel().getColumn(i);
			int width = i == 0? 200 : 1;
			Component comp = jmFilesTable.getDefaultRenderer(String.class).getTableCellRendererComponent(jmFilesTable, column.getHeaderValue(), false, false, 0, i);
			width = Math.max(width, comp.getPreferredSize().width);

			for (int line = 0; line < data.length; ++line) {
				comp = jmFilesTable.getDefaultRenderer(String.class).getTableCellRendererComponent(jmFilesTable,
						data[line][i],false, false, line, i);
				width = Math.max(width, Math.min(150, comp.getPreferredSize().width));
			}
			column.setPreferredWidth(width);
		}
		jmFilesTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				JTable table =(JTable) me.getSource();
				Point p = me.getPoint();
				int row = table.rowAtPoint(p);
				if (me.getClickCount() >= 2) {
					table.getSelectionModel().setSelectionInterval(row, row);
					jmOkButtonActionPerformed(null);
				}
			}
		});
	}

	private void initConnectionDialog(boolean all) {
		DbConnectionDialog dialog = new DbConnectionDialog(this, JailerVersion.APPLICATION_NAME, all? infoBarConnection : infoBarRecUsedConnection, executionContext, false, !all) {
			@Override
			protected boolean isAssignedToDataModel(String dataModelFolder) {
				return modelList.contains(dataModelFolder);
			}
			@Override
			protected void onConnect(ConnectionInfo currentConnection) {
				if (!modelList.contains(currentConnection.dataModelFolder)) {
					JOptionPane.showMessageDialog(DataModelManagerDialog.this,
							"Data Model \"" + currentConnection.dataModelFolder + "\" does not exist.\n");
				} else {
					DataModelManager.setCurrentModelSubfolder(currentConnection.dataModelFolder, executionContext);
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					dbConnectionDialog.currentConnection = currentConnection;
					dbConnectionDialog.isConnected = true;
					store();
					onSelect(dbConnectionDialog, executionContext);
					UISettings.store(tabPropertyName, jTabbedPane1.getSelectedIndex());
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					DataModelManagerDialog.this.setVisible(false);
					DataModelManagerDialog.this.dispose();
				}
			}
		};
		dialog.closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		if (all) {
			dbConnectionDialog = dialog;
			connectionDialogPanel.removeAll();
			connectionDialogPanel.add(dialog.mainPanel);
			dialog.borderPanel.setBorder(null);
		} else {
			recUsedConnectionDialog = dialog;
			recUsedConnectionDialogPanel.removeAll();
			recUsedConnectionDialogPanel.add(dialog.mainPanel);
		}
	}

	private void updateLocationComboboxModel() {
		List<String> existingBaseFolders = new ArrayList<String>();
		for (String bf: baseFolders) {
			if (new File(bf).exists()) {
				existingBaseFolders.add(bf);
			}
		}
		locationComboBox.setModel(new DefaultComboBoxModel<String>(existingBaseFolders.toArray(new String[0])));
	}

	private void loadModelList() {
		DataModelManager.setCurrentModelSubfolder(null, executionContext);

		modelList = new ArrayList<String>();
		modelDetails = new HashMap<String, Pair<String,Long>>();
		for (String mf: DataModelManager.getModelFolderNames(executionContext)) {
			String modelFolder = mf == null? "" : mf;
			modelList.add(modelFolder);
			modelDetails.put(modelFolder, DataModelManager.getModelDetails(mf, executionContext));
		}
		Collections.sort(modelList, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return modelDetails.get(o1).a.compareToIgnoreCase(modelDetails.get(o2).a);
			}
		});
	}

	/**
	 * Initializes the table model.
	 */
	private Object[][] initTableModel() {
		Object[][] data = new Object[modelList.size()][];
		int i = 0;
		for (String model: modelList) {
			Pair<String, Long> details = modelDetails.get(model);
			data[i++] = new Object[] { details == null? "" : details.a, model == null || model.length() == 0? "." : model, details == null? "" : UIUtil.toDateAsString(details.b) };
		}
		DefaultTableModel tableModel = new DefaultTableModel(data, new String[] { "Data Model", "Subfolder", "Last Modified" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			private static final long serialVersionUID = 1535384744352159695L;
		};
		dataModelsTable.setModel(tableModel);
		return data;
	}

	private boolean inRefresh = false;

	/**
	 * Application name. Used to create the name of the demo database alias.
	 */
	private final String applicationName;

	/**
	 * Refreshes the dialog after model changes.
	 */
	private void refresh() {
		if (inRefresh) return;
		inRefresh = true;
		try {
			String theModel = currentModel;
			Object[][] data = initTableModel();
			currentModel = theModel;
			if (currentModel != null) {
				final int i = modelList.indexOf(currentModel);
				if (i >= 0) {
					dataModelsTable.getSelectionModel().setSelectionInterval(i, i);
					UIUtil.invokeLater(new Runnable() {
						@Override
						public void run() {
							Rectangle cellRect = dataModelsTable.getCellRect(i, 1, true);
							dataModelsTable.scrollRectToVisible(cellRect);
						}
					});
				} else {
					dataModelsTable.getSelectionModel().clearSelection();
					currentModel = null;
				}
			} else {
				dataModelsTable.getSelectionModel().clearSelection();
			}
			for (int i = 0; i < dataModelsTable.getColumnCount(); i++) {
				TableColumn column = dataModelsTable.getColumnModel().getColumn(i);
				int width = 1;

				Component comp = dataModelsTable.getDefaultRenderer(String.class).
										getTableCellRendererComponent(
												dataModelsTable, column.getHeaderValue(),
												false, false, 0, i);
				width = Math.max(width, comp.getPreferredSize().width);

				for (int line = 0; line < data.length; ++line) {
					comp = dataModelsTable.getDefaultRenderer(String.class).
									 getTableCellRendererComponent(
											 dataModelsTable, data[line][i],
										 false, false, line, i);
					width = Math.max(width, comp.getPreferredSize().width);
				}

				column.setPreferredWidth(width);
			}
			refreshButtons();
			initConnectionDialog(true);
			initConnectionDialog(false);
			initBookmarkTables();
		} finally {
			inRefresh = false;
		}
	}

	private void refreshButtons() {
		editButton.setEnabled(currentModel != null);
		deleteButton.setEnabled(currentModel != null);
		okButton.setEnabled(currentModel != null);
		analyzeButton.setEnabled(currentModel != null);
	}

	/**
	 * File to store selection.
	 */
	private static String MODEL_SELECTION_FILE = ".selecteddatamodel";

	/**
	 * Stores the selection.
	 */
	private void store() {
		String cm = currentModel;
		if (cm == null) {
			try {
				File file = Environment.newFile(MODEL_SELECTION_FILE);
				if (file.exists()) {
					BufferedReader in = new BufferedReader(new FileReader(file));
					cm = in.readLine();
					in.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		if (cm != null) {
			try {
				File file = Environment.newFile(MODEL_SELECTION_FILE);
				BufferedWriter out = new BufferedWriter(new FileWriter(file));
				out.write(currentModel + "\n");
				out.write(executionContext.getDatamodelFolder() + "\n");
				for (String bf: baseFolders) {
					out.write(bf + "\n");
				}
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads in and set currentBaseFolder.
	 */
	public static void setCurrentBaseFolder(ExecutionContext executionContext) {
		String currentBaseFolder = null;
		try {
			File file = Environment.newFile(MODEL_SELECTION_FILE);
			if (file.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(file));
				String cm = in.readLine();
				if (cm != null) {
					currentBaseFolder = in.readLine();
				}
				in.close();
			}
		} catch (Exception e) {
			// ignore
		}
		if (currentBaseFolder != null && new File(currentBaseFolder).exists()) {
			executionContext.setDatamodelFolder(currentBaseFolder);
		}
	}

	/**
	 * Restores the selection.
	 */
	private void restore() {
		currentModel = null;
		String currentBaseFolder = null;
		baseFolders.clear();
		try {
			File file = Environment.newFile(MODEL_SELECTION_FILE);
			if (file.exists()) {
				BufferedReader in = new BufferedReader(new FileReader(file));
				currentModel = in.readLine();
				if (currentModel != null) {
					currentBaseFolder = in.readLine();
					if (currentBaseFolder != null) {
						for (;;) {
							String bf = in.readLine();
							if (bf == null) {
								break;
							}
							baseFolders.add(bf);
						}
					}
				}
				in.close();
			}
		} catch (Exception e) {
			// ignore
		}
		if (currentBaseFolder == null || !new File(currentBaseFolder).exists()) {
			currentBaseFolder = executionContext.getDatamodelFolder();
		}
		if (executionContext.getDatamodelFolder() != null) {
			if (!baseFolders.contains(executionContext.getDatamodelFolder())) {
				baseFolders.add(0, executionContext.getDatamodelFolder());
			}
		}
		baseFolders.remove(currentBaseFolder);
		baseFolders.add(0, currentBaseFolder);
		executionContext.setDatamodelFolder(currentBaseFolder);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cardPanel = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        browseButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        newButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        analyzeButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        infoBarLabel = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        dataModelsTable = new javax.swing.JTable();
        loadJMPanel = new javax.swing.JPanel();
        infoBarLabel2 = new javax.swing.JLabel();
        loadExtractionModelButton = new javax.swing.JButton();
        jmOkButton = new javax.swing.JButton();
        jmCancelButton = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jmFilesTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        connectionDialogPanel = new javax.swing.JPanel();
        recUsedConnectionPanel = new javax.swing.JPanel();
        recUsedConnectionDialogPanel = new javax.swing.JPanel();
        bookmarkPanel = new javax.swing.JPanel();
        borderPanel = new javax.swing.JPanel();
        bookmarkDialogPanel = new javax.swing.JPanel();
        infoBarLabelBookmark = new javax.swing.JLabel();
        bmCancelButton = new javax.swing.JButton();
        bmOkButton = new javax.swing.JButton();
        recentlyUsedBookmarkPanel = new javax.swing.JPanel();
        borderPanel1 = new javax.swing.JPanel();
        bookmarkRecUsedDialogPanel = new javax.swing.JPanel();
        infoBarLabeRecUsedlBookmark = new javax.swing.JLabel();
        bmRecUsedCancelButton = new javax.swing.JButton();
        bmRecUsedOkButton = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        histLabel = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        restoreButton = new javax.swing.JButton();
        dummyLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Connect with DB");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        cardPanel.setLayout(new java.awt.CardLayout());

        jPanel9.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText(" Loading...");
        jLabel1.setToolTipText("");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 0);
        jPanel9.add(jLabel1, gridBagConstraints);

        cardPanel.add(jPanel9, "loading");

        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel2.setText(" Base Folder  ");
        jPanel4.add(jLabel2, new java.awt.GridBagConstraints());

        browseButton.setText("Change..");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        jPanel4.add(browseButton, gridBagConstraints);

        okButton.setText("Ok");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel4.add(okButton, gridBagConstraints);

        jButton2.setText("Cancel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        jPanel4.add(jButton2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 0);
        jPanel2.add(jPanel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 100;
        gridBagConstraints.gridwidth = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        newButton.setText(" New ");
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 0);
        jPanel3.add(newButton, gridBagConstraints);

        editButton.setText(" Edit ");
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 0);
        jPanel3.add(editButton, gridBagConstraints);

        analyzeButton.setText(" Analyze Database ");
        analyzeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 0);
        jPanel3.add(analyzeButton, gridBagConstraints);

        deleteButton.setText(" Delete ");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 31;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 2, 0);
        jPanel3.add(deleteButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 4);
        jPanel1.add(jPanel3, gridBagConstraints);

        infoBarLabel.setText("info bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel1.add(infoBarLabel, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        dataModelsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(dataModelsTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(jScrollPane2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 8, 0);
        jPanel1.add(jPanel6, gridBagConstraints);

        jTabbedPane1.addTab("Data Model", jPanel1);

        loadJMPanel.setLayout(new java.awt.GridBagLayout());

        infoBarLabel2.setText("info bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        loadJMPanel.add(infoBarLabel2, gridBagConstraints);

        loadExtractionModelButton.setText("Choose File...");
        loadExtractionModelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadExtractionModelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 2);
        loadJMPanel.add(loadExtractionModelButton, gridBagConstraints);

        jmOkButton.setText(" OK ");
        jmOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmOkButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 2);
        loadJMPanel.add(jmOkButton, gridBagConstraints);

        jmCancelButton.setText(" Cancel ");
        jmCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmCancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        loadJMPanel.add(jmCancelButton, gridBagConstraints);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Recent Files"));
        jPanel7.setLayout(new java.awt.GridBagLayout());

        jmFilesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jmFilesTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel7.add(jScrollPane3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        loadJMPanel.add(jPanel7, gridBagConstraints);

        jTabbedPane1.addTab("Extraction Model", loadJMPanel);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        connectionDialogPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel5.add(connectionDialogPanel, gridBagConstraints);

        jTabbedPane1.addTab("Connection", jPanel5);

        recUsedConnectionPanel.setLayout(new java.awt.GridBagLayout());

        recUsedConnectionDialogPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        recUsedConnectionPanel.add(recUsedConnectionDialogPanel, gridBagConstraints);

        jTabbedPane1.addTab("Recently used Connection", recUsedConnectionPanel);

        bookmarkPanel.setLayout(new java.awt.GridBagLayout());

        borderPanel.setLayout(new java.awt.GridBagLayout());

        bookmarkDialogPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        borderPanel.add(bookmarkDialogPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bookmarkPanel.add(borderPanel, gridBagConstraints);

        infoBarLabelBookmark.setText("info bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        bookmarkPanel.add(infoBarLabelBookmark, gridBagConstraints);

        bmCancelButton.setText(" Cancel ");
        bmCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bmCancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        bookmarkPanel.add(bmCancelButton, gridBagConstraints);

        bmOkButton.setText(" OK ");
        bmOkButton.setEnabled(false);
        bmOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bmOkButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 2);
        bookmarkPanel.add(bmOkButton, gridBagConstraints);

        jTabbedPane1.addTab("Bookmark", bookmarkPanel);

        recentlyUsedBookmarkPanel.setLayout(new java.awt.GridBagLayout());

        borderPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Bookmarks"));
        borderPanel1.setLayout(new java.awt.GridBagLayout());

        bookmarkRecUsedDialogPanel.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        borderPanel1.add(bookmarkRecUsedDialogPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        recentlyUsedBookmarkPanel.add(borderPanel1, gridBagConstraints);

        infoBarLabeRecUsedlBookmark.setText("info bar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        recentlyUsedBookmarkPanel.add(infoBarLabeRecUsedlBookmark, gridBagConstraints);

        bmRecUsedCancelButton.setText(" Cancel ");
        bmRecUsedCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bmRecUsedCancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        recentlyUsedBookmarkPanel.add(bmRecUsedCancelButton, gridBagConstraints);

        bmRecUsedOkButton.setText(" OK ");
        bmRecUsedOkButton.setEnabled(false);
        bmRecUsedOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bmRecUsedOkButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 2);
        recentlyUsedBookmarkPanel.add(bmRecUsedOkButton, gridBagConstraints);

        jTabbedPane1.addTab("Recently used Bookmark", recentlyUsedBookmarkPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(jTabbedPane1, gridBagConstraints);

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));
        jPanel11.setPreferredSize(new java.awt.Dimension(1, 8));
        jPanel11.setLayout(new java.awt.GridBagLayout());

        histLabel.setFont(histLabel.getFont().deriveFont(histLabel.getFont().getStyle() | java.awt.Font.BOLD));
        histLabel.setText("History");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 4, 0, 0);
        jPanel11.add(histLabel, gridBagConstraints);

        jPanel12.setLayout(new java.awt.GridBagLayout());

        jPanel10.setBackground(new java.awt.Color(255, 255, 255));
        jPanel10.setLayout(new java.awt.GridBagLayout());

        restoreButton.setText("Restore");
        restoreButton.setFocusCycleRoot(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        jPanel10.add(restoreButton, gridBagConstraints);

        dummyLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        jPanel10.add(dummyLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel12.add(jPanel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel11.add(jPanel12, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel8.add(jPanel11, gridBagConstraints);

        cardPanel.add(jPanel8, "main");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        getContentPane().add(cardPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
		if (currentModel != null) {
			if (JOptionPane.showConfirmDialog(this, "Do you really want to delete Data Model \"" + modelDetails.get(currentModel).a + "\"?", "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				modelList.remove(currentModel);
				DataModelManager.deleteModel(currentModel, executionContext);
				dataModelsTable.getSelectionModel().clearSelection();
				refresh();
				store();
			}
		}
	}//GEN-LAST:event_deleteButtonActionPerformed

	private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
		DataModelManager.setCurrentModelSubfolder(null, executionContext);

		NewDataModelDialog newDataModelDialog = new NewDataModelDialog(this, modelList);

		String newName = newDataModelDialog.getNameEntered();
		if (newName != null) {
			try {
				DataModelManager.createNewModel(newName, newDataModelDialog.getFolderName(), executionContext);
				loadModelList();
				currentModel = newName;
				refresh();
				store();
			} catch (Exception e) {
				UIUtil.showException(this, "Error", e, UIUtil.EXCEPTION_CONTEXT_USER_ERROR);
			}
		}
	}//GEN-LAST:event_newButtonActionPerformed

	private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
		if (currentModel == null) return;
		activateCurrentModel();
		edit(currentModel.length() == 0? null : currentModel);
		loadModelList();
		refresh();
		store();
	}//GEN-LAST:event_editButtonActionPerformed

	private void activateCurrentModel() {
		DataModelManager.setCurrentModelSubfolder(currentModel.length() == 0? null : currentModel, executionContext);
	}

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		close();
	}//GEN-LAST:event_jButton2ActionPerformed

	private void close() {
		hasSelectedModel = false;
		setVisible(false);
		dispose();
	}

	private void analyzeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzeButtonActionPerformed
		if (!UIUtil.canRunJailer()) {
			return;
		}

		activateCurrentModel();

		try {
			JDBCMetaDataBasedModelElementFinder.privilegedSessionProvider = new PrivilegedSessionProviderDialog.Provider(this);
			DbConnectionDialog dbConnectionDialog = new DbConnectionDialog(this, applicationName,
					new DMMDInfoBar("Connect with Database",
							"Select a connection to the database to be analyzed, or create a new connection.\n" +
							"New connections will be assigned to the datamodel \"" + modelDetails.get(currentModel).a + "\".", null), executionContext);
			if (dbConnectionDialog.connect("Analyze Database")) {
				List<String> args = new ArrayList<String>();
				args.add("build-model-wo-merge");
				dbConnectionDialog.addDbArgs(args);

				DataModel dataModel = new DataModel(executionContext);
				AnalyseOptionsDialog analyseOptionsDialog = new AnalyseOptionsDialog(this, dataModel, executionContext);
				boolean[] isDefaultSchema = new boolean[1];
				if (analyseOptionsDialog.edit(dbConnectionDialog, isDefaultSchema, dbConnectionDialog.currentConnection.user)) {
					String schema = analyseOptionsDialog.getSelectedSchema();
					if (schema != null) {
						args.add("-schema");
						args.add(schema);
					}
					if (!isDefaultSchema[0]) {
						args.add("-qualifyNames");
					}
					analyseOptionsDialog.appendAnalyseCLIOptions(args);
					ModelBuilder.assocFilter = analyseOptionsDialog.getAssociationLineFilter();
					if (UIUtil.runJailer(this, args, false, true, true, null, dbConnectionDialog.getUser(), dbConnectionDialog.getPassword(), null, null, false, true, false, null, executionContext)) {
						ModelBuilder.assocFilter = null;
						String modelname = dataModel.getName();
						DataModelEditor dataModelEditor = new DataModelEditor(this, true, analyseOptionsDialog.isRemoving(), null, analyseOptionsDialog.getTableLineFilter(), analyseOptionsDialog.getAssociationLineFilter(), modelname, schema == null? dbConnectionDialog.getName() : schema, dbConnectionDialog, executionContext);
						if (dataModelEditor.dataModelHasChanged()) {
							dataModelEditor.setVisible(true);
						}
					}
					loadModelList();
					refresh();
					store();
				}
			}
		} catch (Exception e) {
			UIUtil.showException(this, "Error", e);
		} finally {
			ModelBuilder.assocFilter = null;
			JDBCMetaDataBasedModelElementFinder.privilegedSessionProvider = null;
		}

	}//GEN-LAST:event_analyzeButtonActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
       	String folder = choseFolder();
       	if (folder != null) {
       		if (new File(folder, DataModel.TABLE_CSV_FILE).exists()) {
       			JOptionPane.showMessageDialog(DataModelManagerDialog.this,
						"\"" + folder + "\"\n" +
       					"is a data model folder.\n\n" +
						"A base folder contains data model folders.", "Invalid Base Folder", JOptionPane.ERROR_MESSAGE);
	   			return;
       		}
       		new File(folder).mkdir();
       		executionContext.setDatamodelFolder(folder);
       		baseFolders.remove(folder);
       		baseFolders.add(0, folder);
       		updateLocationComboboxModel();
    		locationComboBox.setSelectedItem(executionContext.getDatamodelFolder());
			loadModelList();
			refresh();
			store();
       	}
    }//GEN-LAST:event_browseButtonActionPerformed

    private void loadExtractionModelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExtractionModelButtonActionPerformed
		try {
			String modelFile = UIUtil.choseFile(null, "extractionmodel", "Load Extraction Model", ".jm", this, false, true, false);
			if (modelFile != null && UIUtil.checkFileExistsAndWarn(modelFile, this)) {
				UIUtil.setWaitCursor(this);
				onLoadExtractionmodel(modelFile, executionContext);
				setVisible(false);
				dispose();
			}
		} catch (Throwable t) {
			UIUtil.showException(this, "Error", t);
		} finally {
			UIUtil.resetWaitCursor(this);
		}
		UIUtil.checkTermination();
    }//GEN-LAST:event_loadExtractionModelButtonActionPerformed

    private void jmOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmOkButtonActionPerformed
		if (jmFilesTable.getSelectedRow() >= 0) {
			final File file = fileList.get(jmFilesTable.getSelectedRow());
			UIUtil.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						UIUtil.setWaitCursor(DataModelManagerDialog.this);
						onLoadExtractionmodel(file.getPath(), executionContext);
						UISettings.store(tabPropertyName, jTabbedPane1.getSelectedIndex());
						setVisible(false);
						dispose();
					} catch (Throwable t) {
						UIUtil.showException(DataModelManagerDialog.this, "Error", t);
					} finally {
						UIUtil.resetWaitCursor(DataModelManagerDialog.this);
					}
					UIUtil.checkTermination();
				}
			});
		}
	}//GEN-LAST:event_jmOkButtonActionPerformed

    private void jmCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmCancelButtonActionPerformed
		close();
    }//GEN-LAST:event_jmCancelButtonActionPerformed

    private void bmCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bmCancelButtonActionPerformed
		close();
    }//GEN-LAST:event_bmCancelButtonActionPerformed

    private void bmOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bmOkButtonActionPerformed
    }//GEN-LAST:event_bmOkButtonActionPerformed

    private void bmRecUsedCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bmRecUsedCancelButtonActionPerformed
        close();
    }//GEN-LAST:event_bmRecUsedCancelButtonActionPerformed

    private void bmRecUsedOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bmRecUsedOkButtonActionPerformed
    }//GEN-LAST:event_bmRecUsedOkButtonActionPerformed

	/**
	 * Opens file chooser.
	 */
	public String choseFolder() {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle("Base Folder");
		int returnVal = fc.showOpenDialog(this);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	return fc.getSelectedFile().getAbsolutePath();
	    }
		return null;
	}

	/**
	 * Opens data model editor.
	 */
	private void edit(String modelFolder) {
		try {
			DataModelEditor dataModelEditor = new DataModelEditor(this, false, false, null, null, null, modelDetails.get(modelFolder == null? "" : modelFolder).a, null, dbConnectionDialog, executionContext);
			dataModelEditor.setVisible(true);
		} catch (Exception e) {
			UIUtil.showException(this, "Error", e);
		}
	}

	public void start() {
		String datamodelFolder = CommandLineInstance.getInstance().datamodelFolder;
		if (datamodelFolder != null && CommandLineInstance.getInstance().arguments.isEmpty()) {
			executionContext.setDatamodelFolder(new File(datamodelFolder).getParent());
			executionContext.setCurrentModelSubfolder(new File(datamodelFolder).getName());
			UIUtil.prepareUI();
			onSelect(dbConnectionDialog, executionContext);
		} else {
			setVisible(true);
			((CardLayout) cardPanel.getLayout()).show(cardPanel, "loading");
			UIUtil.setWaitCursor(this);
			UIUtil.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						UIUtil.prepareUI();
					} finally {
						((CardLayout) cardPanel.getLayout()).show(cardPanel, "main");
						UIUtil.resetWaitCursor(DataModelManagerDialog.this);
					}
				}
			});
		}
	}

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
		if (currentModel == null) {
			return;
		}

		activateCurrentModel();

		hasSelectedModel = true;
		UIUtil.setWaitCursor(this);
		try {
			store();
			onSelect(null, executionContext);
			UISettings.store(tabPropertyName, jTabbedPane1.getSelectedIndex());
		} catch (Throwable t) {
			UIUtil.showException(this, "Error", t);
		} finally {
			UIUtil.resetWaitCursor(this);
		}
		setVisible(false);
		dispose();
	}// GEN-LAST:event_okButtonActionPerformed

	protected abstract void onSelect(DbConnectionDialog dbConnectionDialog, ExecutionContext executionContext);
	protected abstract void onLoadExtractionmodel(String modelFile, ExecutionContext executionContext2);

	private void openBookmark(BookmarkId bookmark, ConnectionInfo ci) {
		DataModelManager.setCurrentModelSubfolder(bookmark.datamodelFolder, executionContext);
		UIUtil.setWaitCursor(DataModelManagerDialog.this);
		String oldBookmark = CommandLineInstance.getInstance().bookmark;
		String oldRawSchemaMapping = CommandLineInstance.getInstance().rawschemamapping;
		String driver = CommandLineInstance.getInstance().driver;
		String jdbcjar = CommandLineInstance.getInstance().jdbcjar;
		String jdbcjar2 = CommandLineInstance.getInstance().jdbcjar2;
		String jdbcjar3 = CommandLineInstance.getInstance().jdbcjar3;
		String jdbcjar4 = CommandLineInstance.getInstance().jdbcjar4;
		String password = CommandLineInstance.getInstance().password;
		String url = CommandLineInstance.getInstance().url;
		String user = CommandLineInstance.getInstance().user;
		try {
			if (bookmark.getContent() != null) {
				BufferedWriter out = new BufferedWriter(new FileWriter(Environment.newFile(DataBrowser.LAST_SESSION_FILE)));
				out.write(bookmark.getContent());
				out.close();
			}
			CommandLineInstance.getInstance().bookmark = bookmark.bookmark;
			CommandLineInstance.getInstance().rawschemamapping = bookmark.rawSchemaMapping;
			if (ci != null) {
				CommandLineInstance.getInstance().alias = ci.alias;
				CommandLineInstance.getInstance().driver = ci.driverClass;
				CommandLineInstance.getInstance().jdbcjar = ci.jar1;
				CommandLineInstance.getInstance().jdbcjar2 = ci.jar2;
				CommandLineInstance.getInstance().jdbcjar3 = ci.jar3;
				CommandLineInstance.getInstance().jdbcjar4 = ci.jar4;
				CommandLineInstance.getInstance().password = ci.password;
				CommandLineInstance.getInstance().url = ci.url;
				CommandLineInstance.getInstance().user = ci.user;
			}
			store();
			onSelect(null, executionContext);
			UISettings.store(tabPropertyName, jTabbedPane1.getSelectedIndex());
			if (ci != null) {
				UISettings.addRecentConnectionAliases(ci.alias);
			}
			if ("B".equals(module)) {
				UISettings.addRecentBookmarks(bookmark);
			}
		} catch (Throwable t) {
			CommandLineInstance.getInstance().bookmark = oldBookmark;
			CommandLineInstance.getInstance().rawschemamapping = oldRawSchemaMapping;
			CommandLineInstance.getInstance().driver = driver;
			CommandLineInstance.getInstance().jdbcjar = jdbcjar;
			CommandLineInstance.getInstance().jdbcjar2 = jdbcjar2;
			CommandLineInstance.getInstance().jdbcjar3 = jdbcjar3;
			CommandLineInstance.getInstance().jdbcjar4 = jdbcjar4;
			CommandLineInstance.getInstance().password = password;
			CommandLineInstance.getInstance().url = url;
			CommandLineInstance.getInstance().user = user;
			UIUtil.showException(DataModelManagerDialog.this, "Error", t);
		} finally {
			UIUtil.resetWaitCursor(DataModelManagerDialog.this);
		}
	}

    private static class DMMDInfoBar extends InfoBar {
    	
		public DMMDInfoBar(String titel, String message, String footer) {
			super(titel, message, footer);
		}
    	
	    @Override
		public void paint(Graphics g) {
	    	if (g instanceof Graphics2D) {
	    		AffineTransform t = ((Graphics2D) g).getTransform();
	    		if (t != null) {
	    			int s = Math.min(Math.abs((int) (t.getScaleX() * t.getScaleY() * 100)), 999);
	    			if (t.getScaleX() < 0) {
	    				s += 2000;
	    			}
	    			if (t.getScaleY() < 0) {
	    				s += 4000;
	    			}
	    			try {
						String osName = System.getProperty("os.name");
						if (osName != null && osName.toLowerCase().contains("windows")) {
							String sessionName = System.getenv("sessionname");
							if (sessionName != null && sessionName.startsWith("RDP")) {
								s += 1000;
							}
						}
					} catch (Throwable t2) {
						// ignore
					}
	    			UISettings.s11 = s == 100? 0 : s;
	    		}
	    	}
			super.paint(g);
		}
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton analyzeButton;
    private javax.swing.JButton bmCancelButton;
    private javax.swing.JButton bmOkButton;
    private javax.swing.JButton bmRecUsedCancelButton;
    private javax.swing.JButton bmRecUsedOkButton;
    private javax.swing.JPanel bookmarkDialogPanel;
    private javax.swing.JPanel bookmarkPanel;
    private javax.swing.JPanel bookmarkRecUsedDialogPanel;
    private javax.swing.JPanel borderPanel;
    private javax.swing.JPanel borderPanel1;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JPanel connectionDialogPanel;
    private javax.swing.JTable dataModelsTable;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel dummyLabel;
    private javax.swing.JButton editButton;
    private javax.swing.JLabel histLabel;
    private javax.swing.JLabel infoBarLabeRecUsedlBookmark;
    private javax.swing.JLabel infoBarLabel;
    private javax.swing.JLabel infoBarLabel2;
    private javax.swing.JLabel infoBarLabelBookmark;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton jmCancelButton;
    private javax.swing.JTable jmFilesTable;
    private javax.swing.JButton jmOkButton;
    private javax.swing.JButton loadExtractionModelButton;
    private javax.swing.JPanel loadJMPanel;
    private javax.swing.JButton newButton;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel recUsedConnectionDialogPanel;
    private javax.swing.JPanel recUsedConnectionPanel;
    private javax.swing.JPanel recentlyUsedBookmarkPanel;
    private javax.swing.JButton restoreButton;
    // End of variables declaration//GEN-END:variables
    private javax.swing.JComboBox locationComboBox = new JComboBox2();
    private JComboBox2 recentSessionsComboBox = new JComboBox2();

	private static final long serialVersionUID = -3983034803834547687L;
	
	private static ImageIcon okIcon;
	private static ImageIcon cancelIcon;
	private static ImageIcon histIcon;
	
	static {
		// load images
		okIcon = UIUtil.readImage("/buttonok.png");
        cancelIcon = UIUtil.readImage("/buttoncancel.png");
        histIcon = UIUtil.readImage("/history.png");
	}

	// TODO 1
	// TODO single app with module selection here?
	
}
