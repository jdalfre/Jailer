/*
 * ParametersEditor.java
 *
 * Created on 1. Juli 2009, 09:00
 */
package net.sf.jailer.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import net.sf.jailer.datamodel.ParameterHandler;

/**
 * Parameter selector.
 *
 * @author Ralf Wisser
 */
public class ParameterSelector extends javax.swing.JPanel {
	
	public static interface ParametersGetter {
		Set<String> getParameters();
	}
	
	/**
	 * Gets current parameters.
	 */
	private final ParametersGetter parametersGetter;
	
	/**
	 * Parameters list.
	 */
	private List<String> parameters;
	
	/** Creates new form ParametersEditor */
	public ParameterSelector(final Component parent, final JTextComponent textArea, ParametersGetter parametersGetter) {
		this.parametersGetter = parametersGetter;
		parameters = new ArrayList<String>();
		
		initComponents();
		try {
			((DefaultTableCellRenderer) paramTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
		} catch (Exception e) {
			// ignore
		}
		final TableCellRenderer defaultTableCellRenderer = paramTable.getDefaultRenderer(String.class);
		paramTable.setShowGrid(false);
		paramTable.setDefaultRenderer(Object.class,
				new TableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(
							JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Component render = defaultTableCellRenderer
								.getTableCellRendererComponent(table, value,
										isSelected, hasFocus, row, column);
						if (render instanceof JLabel && !isSelected) {
							final Color BG1 = UIUtil.TABLE_BACKGROUND_COLOR_1;
							final Color BG2 = UIUtil.TABLE_BACKGROUND_COLOR_2;
							((JLabel) render)
									.setBackground((row % 2 == 0) ? BG1
											: BG2);
						}
						return render;
					}
				});
		paramTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		paramTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					int lastRow = -2;
					@Override
					public void valueChanged(ListSelectionEvent evt) {
						int row = paramTable.getSelectedRow();
						if (lastRow == row) {
							return;
						}
						lastRow = row;
						if (row == 0) {
							hasOpenDialog = true;
							String newParameter = JOptionPane.showInputDialog(parent, "Parameter", "New Parameter", JOptionPane.QUESTION_MESSAGE);
							hasOpenDialog = false;
							if (newParameter != null) {
								newParameter = newParameter.trim();
								String np = "";
								for (int i = 0; i < newParameter.length(); ++i) {
									char c = newParameter.charAt(i);
									if (ParameterHandler.VALID_CHARS.indexOf(c) >= 0) {
										np = np + c;
									}
								}
								newParameter = np;
								if (newParameter.length() > 0) {
									if (!parameters.contains(newParameter)) {
										parameters.add(newParameter);
									}
									textArea.replaceSelection("${" + newParameter + "}");
									textArea.grabFocus();
									refresh();
								}
							}
						} else if (row > 0) {
							textArea.replaceSelection("${" + parameters.get(row - 1) + "}");
							textArea.grabFocus();
						}
						paramTable.getSelectionModel().clearSelection();
					}
				});
		
		updateParameters();
		refresh();
	}

	/**
	 * Reads current parameters and updates internal parameter list.
	 */
	public void updateParameters() {
		parameters.clear();
		parameters.addAll(parametersGetter.getParameters());
		refresh();
	}
	
	/**
	 * Refreshes table.
	 */
	private void refresh() {
		Collections.sort(parameters);
		Object[][] data = new Object[parameters.size() + 1][];
		int i = 0;
		data[i++] = new Object[] { "<new parameter>" };
		for (String s: parameters) {
			data[i++] = new Object[] { s };
		}
		paramTable.setModel(new DefaultTableModel(data, new Object[] { "Parameter" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			private static final long serialVersionUID = 2703862797772451362L;
		});
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		jPanel1 = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		paramTable = new javax.swing.JTable();

		setLayout(new java.awt.GridLayout(1, 1));

		jPanel1.setBorder(null);
		jPanel1.setLayout(new java.awt.GridBagLayout());

		paramTable.setModel(new javax.swing.table.DefaultTableModel(
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
		jScrollPane2.setViewportView(paramTable);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		jPanel1.add(jScrollPane2, gridBagConstraints);

		add(jPanel1);
	}// </editor-fold>//GEN-END:initComponents
	
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	javax.swing.JPanel jPanel1;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JTable paramTable;
	// End of variables declaration//GEN-END:variables
	
	private static final long serialVersionUID = 5153763345779925099L;
	private boolean hasOpenDialog = false;
	
	public boolean hasOpenDialog() {
		return hasOpenDialog;
	}
}
