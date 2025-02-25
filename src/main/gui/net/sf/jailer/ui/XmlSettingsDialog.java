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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;

import net.sf.jailer.datamodel.DataModel;

/**
 * Dialog for XML settings.
 * 
 * @author Ralf Wisser
 */
public class XmlSettingsDialog extends javax.swing.JDialog {
	
	/** Creates new form XmlSettingsDialog */
	public XmlSettingsDialog(java.awt.Frame parent) {
		super(parent, true);
		initComponents();
		setModal(true);
		UIUtil.setInitialWindowLocation(this, parent, 100, 150);
		Ok.setIcon(UIUtil.scaleIcon(Ok, okIcon));
		cancelButton.setIcon(UIUtil.scaleIcon(cancelButton, cancelIcon));
		datePattern.getEditor().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateExamples();
			}
		});
		timestampPattern.getEditor().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateExamples();
			}
		});
		datePattern.setModel(new DefaultComboBoxModel(datePatternOptions));
		timestampPattern.setModel(new DefaultComboBoxModel(timePatternOptions));
		timestampPattern.getEditor().setItem("yyyy.MM.dd.-H.mm.ss   ");
		timestampExample.setText("yyyy.MM.dd.-H.mm.ss   ");
		pack();
	}
	
	private boolean okClicked;
	
	/**
	 * Edits the XML settings of a data model.
	 * 
	 * @param dataModel the data model
	 * @return <code>true</code> iff settings are changed
	 */
	public boolean edit(DataModel dataModel) {
		DataModel.XmlSettings xmlSettings = dataModel.getXmlSettings();
		
		datePattern.getEditor().setItem(xmlSettings.datePattern);
		timestampPattern.getEditor().setItem(xmlSettings.timestampPattern);
		rootTag.setText(xmlSettings.rootTag);
		noRootTag.setSelected("".equals(xmlSettings.rootTag));
		rootTag.setEditable(!"".equals(xmlSettings.rootTag));
		updateExamples();
		
		okClicked = false;
		setVisible(true);
		if (okClicked) {
			boolean change = false;
			String newDatePattern = datePattern.getEditor().getItem().toString();
			String newTimestampPattern = timestampPattern.getEditor().getItem().toString();
			String newRootTag = rootTag.getText().trim();
			if (noRootTag.isSelected()) {
				newRootTag = "";
			}
			if (!newDatePattern.equals(xmlSettings.datePattern)) {
				xmlSettings.datePattern = newDatePattern;
				change = true;
			}
			if (!newTimestampPattern.equals(xmlSettings.timestampPattern)) {
				xmlSettings.timestampPattern = newTimestampPattern;
				change = true;
			}
			if (!newRootTag.equals(xmlSettings.rootTag)) {
				xmlSettings.rootTag = newRootTag;
				change = true;
			}
			return change;
		}
		return false;
	}

	private void updateExamples() {
		Date now = new Date();
		String dateExampleText = "- invalid pattern -";
		String timestampExampleText = "- invalid pattern -";
		try {
			dateExampleText = new SimpleDateFormat(datePattern.getEditor().getItem().toString(), Locale.ENGLISH).format(now);
		} catch (Exception e) {
			// ignore
		}
		try {
			timestampExampleText = new SimpleDateFormat(timestampPattern.getEditor().getItem().toString(), Locale.ENGLISH).format(now);
		} catch (Exception e) {
			// ignore
		}
		dateExample.setText(dateExampleText);
		timestampExample.setText(timestampExampleText);
	}
	
	private static String[] datePatternOptions = new String[] {
		"dd-MM-yyyy",
		"dd/MM/yyyy", "dd.MM.yyyy", "dd.MMM.yyyy", "dd MMMM yyyy",
		"MM-dd-yyyy", "MM/dd/yyyy", "MMM d, yyyy", "yyyy.d.M",
		"dd MMM yyyy", "dd-MMM-yyyy", "dd.MM.yyyy.", "d MMM yyyy",
		"d MMM, yyyy", "d-MMM-yyyy", "d/MMM/yyyy", "d/MM/yyyy",
		"d.MM.yyyy", "d.M.yyyy", "Gy.MM.dd", "yyyy-M-d", "yyyy/M/d",
		"yyyy. M. d", "yyyy.M.d", "yyyy'?'M'?'d'?'", "yyyy-MM-dd",
		"yyyy/MM/dd", "yyyy.MM.dd", "yyyy.MM.dd.", "yyyy MMM d",
		"yyyy-MMM-dd", "dd MMM yy", "dd-MMM-yy", "MM d, yy" };

	private static String[] timePatternOptions = new String[] {
		"dd/MM/yyyy HH:mm:ss", "dd.MM.yyyy HH:mm:ss",
		"dd.MM.yyyy. HH.mm.ss", 
		"dd.MM.yyyy H:mm:ss",
		"yyyy-MM-dd HH:mm:ss",
		"yyyy.MM.dd HH:mm:ss", "yyyy/MM/dd H:mm:ss", "yyyy.MM.dd. H:mm:ss",
		"yyyy-MMM-dd HH:mm:ss",

		"dd/MM/yyyy HH.mm.ss", "dd.MM.yyyy HH.mm.ss",
		"dd.MM.yyyy. HH.mm.ss", "dd.MM.yyyy H.mm.ss",
		"yyyy-MM-dd HH.mm.ss",
		"yyyy.MM.dd HH.mm.ss", "yyyy/MM/dd H.mm.ss", "yyyy.MM.dd. H.mm.ss",
		"yyyy-MMM-dd HH.mm.ss",

		"dd/MM/yyyy-HH.mm.ss", "dd.MM.yyyy-HH.mm.ss",
		"dd.MM.yyyy.-HH.mm.ss", 
		"dd.MM.yyyy-H.mm.ss",
		"yyyy-MM-dd-HH.mm.ss",
		"yyyy.MM.dd-HH.mm.ss", "yyyy/MM/dd-H.mm.ss", "yyyy.MM.dd.-H.mm.ss",
		"yyyy-MMM-dd-HH.mm.ss",

		"dd MMM yy H:mm:ss",
		"dd MMM yyyy HH:mm:ss", "dd-MMM-yyyy HH:mm:ss",
		"dd.MMM.yyyy HH:mm:ss", "dd-MMM-yyyy H:mm:ss",
		"dd-MM-yyyy HH:mm:ss",
		"d MMM yyyy HH:mm:ss", "d-MMM-yyyy HH:mm:ss", "d MMM yyyy H:mm:ss",
		"d MMM yyyy, H:mm:ss", "d-MMM-yyyy H:mm:ss", "d-MMM-yyyy H.mm.ss",
		"d/MMM/yyyy H:mm:ss",
		"d/MM/yyyy HH:mm:ss", "d.MM.yyyy H:mm:ss",
		"d.M.yyyy HH:mm:", "d.M.yyyy HH:mm:ss", "d.M.yyyy H:mm:ss",
		"d.M.yyyy H.mm.ss", "Gy.MM.dd H:mm:ss", "HH:mm:ss dd-MM-yyyy",
		"HH:mm:ss dd/MM/yyyy",
		"yyyy.d.M HH:mm:ss",
		"yyyy.M.d HH.mm.ss",
		"yyyy MMM d HH:mm:ss" };

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        datePattern = new javax.swing.JComboBox();
        timestampPattern = new javax.swing.JComboBox();
        rootTag = new javax.swing.JTextField();
        dateExample = new javax.swing.JLabel();
        timestampExample = new javax.swing.JLabel();
        noRootTag = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        Ok = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("XML Settings");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel1, gridBagConstraints);

        jLabel1.setText("Date pattern ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Timestamp pattern ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Root tag");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(jLabel3, gridBagConstraints);

        datePattern.setEditable(true);
        datePattern.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Eintrag 1", "Eintrag 2", "Eintrag 3", "Eintrag 4" }));
        datePattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                datePatternActionPerformed(evt);
            }
        });
        datePattern.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                datePatternKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(datePattern, gridBagConstraints);

        timestampPattern.setEditable(true);
        timestampPattern.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Eintrag 1", "Eintrag 2", "Eintrag 3", "Eintrag 4" }));
        timestampPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timestampPatternActionPerformed(evt);
            }
        });
        timestampPattern.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                timestampPatternKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(timestampPattern, gridBagConstraints);

        rootTag.setText("jTextField1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        jPanel3.add(rootTag, gridBagConstraints);

        dateExample.setText("jLabel4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 20;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        jPanel3.add(dateExample, gridBagConstraints);

        timestampExample.setText("jLabel5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 20;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        jPanel3.add(timestampExample, gridBagConstraints);

        noRootTag.setText("no root tag");
        noRootTag.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                noRootTagItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel3.add(noRootTag, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        Ok.setText("OK");
        Ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OkActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 2);
        jPanel2.add(Ok, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        jPanel2.add(cancelButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 100;
        gridBagConstraints.gridwidth = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 2, 0);
        jPanel3.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        getContentPane().add(jPanel3, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void timestampPatternKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_timestampPatternKeyTyped
		updateExamples();
	}//GEN-LAST:event_timestampPatternKeyTyped

	private void timestampPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timestampPatternActionPerformed
		updateExamples();
	}//GEN-LAST:event_timestampPatternActionPerformed

	private void datePatternKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_datePatternKeyTyped
		updateExamples();
	}//GEN-LAST:event_datePatternKeyTyped

	private void datePatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_datePatternActionPerformed
		updateExamples();
	}//GEN-LAST:event_datePatternActionPerformed

	private void OkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OkActionPerformed
		okClicked = true;
		setVisible(false);
	}//GEN-LAST:event_OkActionPerformed

	private void noRootTagItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_noRootTagItemStateChanged
		rootTag.setEditable(!noRootTag.isSelected());
	}//GEN-LAST:event_noRootTagItemStateChanged

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		dispose();
	}//GEN-LAST:event_cancelButtonActionPerformed
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Ok;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel dateExample;
    private javax.swing.JComboBox datePattern;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JCheckBox noRootTag;
    private javax.swing.JTextField rootTag;
    private javax.swing.JLabel timestampExample;
    private javax.swing.JComboBox timestampPattern;
    // End of variables declaration//GEN-END:variables
	
	private static ImageIcon okIcon;
	private static ImageIcon cancelIcon;
	
	static {
        // load images
        okIcon = UIUtil.readImage("/buttonok.png");
        cancelIcon = UIUtil.readImage("/buttoncancel.png");
	}

	private static final long serialVersionUID = -2752715206964965549L;
}
