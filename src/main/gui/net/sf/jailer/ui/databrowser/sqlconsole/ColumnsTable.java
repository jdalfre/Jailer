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
package net.sf.jailer.ui.databrowser.sqlconsole;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.sf.jailer.ui.UIUtil;
import net.sf.jailer.ui.databrowser.BrowserContentPane;
import net.sf.jailer.ui.databrowser.DetailsView;
import net.sf.jailer.ui.databrowser.Row;

/**
 * "Column view" of a query result table.
 *  
 * @author Ralf Wisser
 */
public class ColumnsTable extends JTable {
	private static final long serialVersionUID = 1L;

	private final int MAX_ROWS = 498;
	private static final KeyStroke KS_COPY_TO_CLIPBOARD = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
	final BrowserContentPane rb;
	private Map<Integer, String> tableName = new HashMap<Integer, String>();
	private boolean useTableName = true;
	private final boolean inDesktop;
	private boolean inClosure;
	private int currentRow = -1;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ColumnsTable(final BrowserContentPane rb, boolean inDesktop) {
		this.rb = rb;
		this.inDesktop = inDesktop;
		final JTable rowsTable = rb.rowsTable;

		final RowSorter<? extends TableModel> sorter = rowsTable.getRowSorter();
		Vector cNames = new Vector();
		cNames.add("Column");
		for (int i = 0; i < sorter.getViewRowCount(); ++i) {
			if (i > MAX_ROWS) {
				cNames.add("Row " + (i + 1) + " (" + (sorter.getViewRowCount() - i - 1) + " more)");
				break;
			}
			cNames.add("Row " + (i + 1));
		}
		final TableColumnModel cm = rowsTable.getColumnModel();
		TableModel cDm = new DefaultTableModel(cNames, rowsTable.getModel().getColumnCount()) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				if (columnIndex == 0) {
					return false;
				}
				int row = rowsTable.getRowSorter().convertRowIndexToModel(columnIndex - 1);
				int column = cm.getColumn(rowIndex).getModelIndex();
				return rowsTable.getModel().isCellEditable(row, column);
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				int column = cm.getColumn(rowIndex).getModelIndex();
				if (columnIndex == 0) {
					String[] ntPair = rowsTable.getModel().getColumnName(column).replaceAll("<br>", "\t").replaceAll("<[^>]*>", "").split("\t");
					if (ntPair.length == 1) {
						return UIUtil.fromHTMLFragment(ntPair[0]);
					}
					if (ntPair.length == 2) {
						return UIUtil.fromHTMLFragment(ntPair[0]);
					}
					if (ntPair.length == 3) {
						tableName.put(rowIndex, ntPair[0]);
						return UIUtil.fromHTMLFragment(ntPair[1]);
					}
					return ntPair;
				}
				int row = rowsTable.getRowSorter().convertRowIndexToModel(columnIndex - 1);
				return rowsTable.getModel().getValueAt(row, column);
			}
			
			@Override
			public void setValueAt(Object value, int rowIndex, int columnIndex) {
				if (columnIndex == 0) {
					return;
				}
				int row = rowsTable.getRowSorter().convertRowIndexToModel(columnIndex - 1);
				int column = cm.getColumn(rowIndex).getModelIndex();
				rowsTable.getModel().setValueAt(value, row, column);
			}
		};

		if (!inDesktop) {
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			setCellSelectionEnabled(true);
		} else {
			setFocusable(false);
			setRowSelectionAllowed(false);
			setColumnSelectionAllowed(false);
			rowsTable.setEnabled(false);
			rowsTable.setAutoscrolls(false);			
		}
		// getTableHeader().setReorderingAllowed(false);
		setShowGrid(false);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setModel(cDm);
		
		for (int row = 0; row < cDm.getRowCount(); ++row) {
			cDm.getValueAt(row, 0);
		}
		int cnt[] = new int[1];
		new HashSet<String>(tableName.values()).forEach(s -> {
			if (!s.matches("^(<[^>]+>)*((&nbsp;)*)(<[^>]+>)*$")) {
				++cnt[0];
			}
		});
		if (cnt[0] <= 1) {
			useTableName = false;
		}
		
		for (int i = 0; i < getColumnCount(); i++) {
			TableCellEditor defaultEditor = rowsTable.getDefaultEditor(getColumnClass(i));
			if (defaultEditor != null) {
				defaultEditor.cancelCellEditing();
			}
			setDefaultEditor(getColumnClass(i), defaultEditor);
		}
		TableCellEditor defaultEditor = rowsTable.getDefaultEditor(Object.class);
		setDefaultEditor(Object.class, defaultEditor);
		InputMap im = getInputMap();
		Object key = "copyClipboard";
		im.put(KS_COPY_TO_CLIPBOARD, key);
		ActionMap am = getActionMap();
		Action a = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				UIUtil.copyToClipboard(ColumnsTable.this, false);
			}
		};
		am.put(key, a);

		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (!inDesktop) {
					int ri = rowAtPoint(e.getPoint());
					if (ri >= 0) {
						Rectangle r = getCellRect(ri, 0, false);
						int x = Math.max(e.getPoint().x, (int) r.getMinX());
						int y = (int) r.getMaxY() - 2;
						if (e.getButton() != MouseEvent.BUTTON1) {
							JPopupMenu popup;
							popup = createPopupMenu(e);
							if (popup != null) {
								popup.addPropertyChangeListener("visible", new PropertyChangeListener() {
						    		@Override
									public void propertyChange(PropertyChangeEvent evt) {
						    			if (Boolean.FALSE.equals(evt.getNewValue())) {
						    				rb.setCurrentRowSelection(-1);
						    				repaint();
						    			}
						    		}
								});
						    	popup.show(ColumnsTable.this, x, y);
							}
						} else if (e.getClickCount() > 1) {
							int i = -1;
							ri = columnAtPoint(e.getPoint()) - 1;
							if (ri >= 0 && !rb.rows.isEmpty() && rb.rowsTable.getRowSorter().getViewRowCount() > 0) {
								i = rb.rowsTable.getRowSorter().convertRowIndexToModel(ri);
								Point p = new Point(e.getX(), e.getY());
								SwingUtilities.convertPointToScreen(p, ColumnsTable.this);
								rb.openDetails(i, (int) p.getX(), (int) p.getY());
							}
						}
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});

		setDefaultRenderer(Object.class, new TableCellRenderer() {
			final Color BGCOLUMNS = new Color(255, 255, 230);
			final Color BGSELECTED  = new Color(255, 230, 220);
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				int dmColumn = column;
				if (dmColumn  > 0) {
					--dmColumn;
				}
				Component render = rowsTable.getCellRenderer(dmColumn, row).getTableCellRendererComponent(ColumnsTable.this, value, isSelected, hasFocus, dmColumn, row);
				int currentColumn = rb.getCurrentRowSelection();
				if (render instanceof JLabel) {
					if (column == 0) {
						((JLabel) render).setBackground(BGCOLUMNS);
					} else if (ColumnsTable.this.inDesktop && !"final".equals(render.getName())) {
						if (ColumnsTable.this.inClosure && !inTempClosure()) {
							((JLabel) render).setBackground(row % 2 == 0? DetailsView.BG3 : DetailsView.BG3_2);
						} else {
							((JLabel) render).setBackground(row % 2 == 0? DetailsView.BG1 : DetailsView.BG2);
						}
					} else if (column - 1 == currentColumn && !"final".equals(render.getName())) {
						((JLabel) render).setBackground(BGSELECTED);
					}
					if (column == 0) {
						String text = ((JLabel) render).getText();
						String tabName = useTableName? tableName.get(row) : null;
						if (tabName != null) {
							JLabel tab = new JLabel("<html>&nbsp;" + tabName + "&nbsp;</html>");
							tab.setForeground(new Color(0, 0, 180));
							tab.setBackground(render.getBackground());
							tab.setOpaque(render.isOpaque());
							JPanel panel = new JPanel(new GridBagLayout());
							panel.setToolTipText(text);
							GridBagConstraints gridBagConstraints;
							gridBagConstraints = new java.awt.GridBagConstraints();
					        gridBagConstraints.gridx = 1;
					        gridBagConstraints.gridy = 1;
					        gridBagConstraints.fill = GridBagConstraints.BOTH;
					        gridBagConstraints.weightx = 1;
					        gridBagConstraints.weighty = 1;
					        gridBagConstraints.anchor = GridBagConstraints.WEST;
					        panel.add(tab, gridBagConstraints);
					        gridBagConstraints = new java.awt.GridBagConstraints();
					        gridBagConstraints.gridx = 3;
					        gridBagConstraints.gridy = 1;
					        gridBagConstraints.anchor = GridBagConstraints.EAST;
					        gridBagConstraints.fill = GridBagConstraints.BOTH;
					        gridBagConstraints.weighty = 1;
					        panel.add(render, gridBagConstraints);
					        ((JLabel) render).setText(text + "  ");
							return panel;
						}
						((JLabel) render).setText(" " + text);
					} else {
						if (inTempClosure()) {
							((JLabel) render).setBackground(blend(((JLabel) render).getBackground()));
						}
					}
					if ("found".equals(render.getName())) {
						Color background = render.getBackground();
						render.setBackground(
								new Color(
										Math.max((int)(background.getRed()), 0),
										Math.max((int)(background.getGreen() * 0.90), 0),
										Math.max((int)(background.getBlue() * 0.91), 0),
										background.getAlpha()));
					}
				}
				if (currentRow == row) {
					render.setBackground(new Color(122, 210, 255, 200));
				}
				return render;
			}
			private Color blend(Color color1) {
				int alpha = 14;
				Color color2 = new Color(255, 0, 0);
				float factor = alpha / 255f;
				int red = (int) (color1.getRed() * (1 - factor) + color2.getRed() * factor);
				int green = (int) (color1.getGreen() * (1 - factor) + color2.getGreen() * factor);
				int blue = (int) (color1.getBlue() * (1 - factor) + color2.getBlue() * factor);
				return new Color(red, green, blue);
			}
		});
		adjustTableColumnsWidth();
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				currentRow = rowAtPoint(e.getPoint());
				if (fixed != null) {
					fixed.repaint();
				}
				repaint();
			}
		});
	}

	/**
	 * Creates popup menu. 
	 * @param e mouse event 
	 */
	private JPopupMenu createPopupMenu(MouseEvent e) {
		int i = -1;
		Row row = null;
		int ri = columnAtPoint(e.getPoint()) - 1;
		if (ri >= 0 && !rb.rows.isEmpty() && rb.rowsTable.getRowSorter().getViewRowCount() > 0) {
			i = rb.rowsTable.getRowSorter().convertRowIndexToModel(ri);
			row = rb.rows.get(i);
			rb.setCurrentRowSelection(ri);
			repaint();
		} else {
			return null;
		}
		JMenuItem copyTCB = null; // new JMenuItem("Copy to Clipboard");
//		// copyTCB.setAccelerator(KS_COPY_TO_CLIPBOARD);
//		copyTCB.setEnabled(getSelectedColumnCount() > 0);
//		copyTCB.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				UIUtil.copyToClipboard(ColumnsTable.this, false);
//			}
//		});
		Point p = new Point(e.getX(), e.getY());
		SwingUtilities.convertPointToScreen(p, this);
		return rb.createPopupMenu(this, row, i, (int) p.getX(), (int) p.getY(), false, copyTCB, new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < getColumnCount(); i++) {
					TableCellEditor defaultEditor = getDefaultEditor(getColumnClass(i));
					if (defaultEditor != null) {
						defaultEditor.cancelCellEditing();
					}
				}
			}
		}, false, true, true);
	}

	private void adjustTableColumnsWidth() {
		DefaultTableModel dtm = (DefaultTableModel) getModel();
		DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();
		int maxWidth = getColumnCount() > 2? 300 : 1200;
		for (int i = 0; i < getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			int width = 0;

			Component comp = defaultTableCellRenderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, 0, i);
			int pw = comp.getPreferredSize().width;
			if (pw < 100) {
				pw = (pw * 130) / 100 + 10;
			}
			width = Math.max(width, pw);

			int line = 0;
			for (; line < getRowCount(); ++line) {
				comp = getCellRenderer(line, i).getTableCellRendererComponent(this, dtm.getValueAt(line, i), false, false, line, i);
				width = Math.max(width, comp.getPreferredSize().width + 16);
			}
			Object maxValue = null;
			int maxValueLength = 0;
			for (; line < getRowCount(); ++line) {
				Object value = dtm.getValueAt(line, i);
				if (value != null) {
					int valueLength = value.toString().length();
					if (maxValue == null || maxValueLength < valueLength) {
						maxValue = value;
						maxValueLength = valueLength;
					}
				}
			}
			if (maxValue != null) {
				comp = getCellRenderer(line, i).getTableCellRendererComponent(this, maxValue, false, false, line, i);
				int maxValueWidth = comp.getPreferredSize().width + 16;
				if (maxValueWidth > width) {
					width = maxValueWidth;
				}
			}
			int prefWidth = Math.min(maxWidth, width);
			if (i == 0) {
				prefWidth = Math.min(prefWidth, 260);
			}
			column.setPreferredWidth(prefWidth);
		}
	}

	public void scrollToCurrentRow() {
		final int currentColumn = rb.getCurrentRowSelection();
		if (currentColumn >= 0 && currentColumn + 1 < getColumnCount()) {
			UIUtil.invokeLater(2, new Runnable() {
				@Override
				public void run() {
					Rectangle cellRect = getCellRect(0, currentColumn + 1, true);
					Rectangle cellRectLast = getCellRect(0, getColumnCount() - 1, true);
					Rectangle visRect = getVisibleRect();
					int b = 0; // Math.max(cellRect.width / 4, 16);
					if (cellRect.x < visRect.x || cellRect.x + cellRect.width > visRect.x + visRect.width) {
						scrollRectToVisible(new Rectangle(Math.max(cellRectLast.x + cellRectLast.width + b, 1), visRect.y + visRect.height / 2, 1, Math.max(cellRect.height + b, 1)));
						scrollRectToVisible(new Rectangle(Math.max(cellRect.x - b, 1), visRect.y + visRect.height / 2, 1, Math.max(cellRect.height + b, 1)));
					}
				}
			});
		}
	}

	public void updateInClosureState(boolean inClosure) {
		this.inClosure = inClosure;
		repaint();
	}
	
	protected boolean inTempClosure() {
		return false;
	}
	
	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
	}

	private JTable fixed;
	
	public void initFixedTable(JTable fixed) {
		this.fixed = fixed;
		fixed.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				currentRow = rowAtPoint(e.getPoint());
				if (fixed != null) {
					fixed.repaint();
				}
				repaint();
			}
		});
	}

}
