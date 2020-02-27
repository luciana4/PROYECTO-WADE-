/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package com.tilab.wade.tools.launcher.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.tilab.wade.performer.Constants;
import com.tilab.wade.performer.descriptors.Parameter;

public class ParametersPanel extends JScrollPane {
	
	private JTable table;
	private AbstractTableModel model;
	private TableCellRenderer renderer;

	private List<Row> rows = new ArrayList<Row>();
	private LauncherGUI launcherGUI;

	public ParametersPanel(LauncherGUI launcherGUI) {
		super();
		this.launcherGUI = launcherGUI;

		renderer = new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				Row r = rows.get(row);
				switch(col) {
				case 0: 
					return r.getName();
				case 1: 
					return r.getType();
				case 2: 
					return r.getMode();
				case 3:
					return r.getValue();
				default:
					return null;
				}
			}
		};

		// Set empty table
		setParameters(new jade.util.leap.ArrayList());
	}
	
	public void setParameters(jade.util.leap.List parameters) {
		rows.clear();
		
		int labelNameMaxWidth = -1;
		for (int i = 0; i < parameters.size(); ++i) {
			Row r = new Row((Parameter) parameters.get(i));
			rows.add(r);
			JLabel labelName = r.getName();
			int width = labelName.getPreferredSize().width;
			if (width > labelNameMaxWidth) {
				labelNameMaxWidth = width;
			}
		}

		model = new AbstractTableModel() {
			public int getColumnCount() {return 4;}
			public int getRowCount() {return rows.size();}

			public String getColumnName(int col) {
				switch (col) {
				case 0:
					return "Name";
				case 1:
					return "Type";
				case 2:
					return "Mode";
				case 3:
					return "Value";
				default:
					return null;
				}
			}
			
			public Object getValueAt(int row, int col) {
				Parameter param = rows.get(row).getParameter();
				switch (col) {
				case 0:
					return param.getName();
				case 1:
					return param.getType();
				case 2:
					return getParameterMode(param.getMode());
				case 3:
					return param.getValue();
				default:
					return null;
				}
			}
			
			public void setValueAt(Object aValue, int row, int col) {
				if (col == 3) {
					Parameter param = rows.get(row).getParameter();
					try {
						if (aValue instanceof String) {
							aValue = decodeValue((String)aValue, param.getType());
						}
					}
					catch (Exception e) {
						JOptionPane.showMessageDialog(launcherGUI, e.getMessage());
						aValue = null;
						rows.get(row).resetValue();
					}
					
					param.setValue(aValue);
					fireTableCellUpdated(row, col);
					launcherGUI.refreshActionsStatus();
				}
			}
			
			private Object decodeValue(String valueStr, String type) throws Exception {
				try {
					Object result = null;
					if(type.equals(String.class.getName())) {
						result = valueStr;
					}
					else if(type.equals(int.class.getName()) || type.equals(long.class.getName()) || type.equals(Integer.class.getName()) || type.equals(Long.class.getName())) {
						result = new Long(valueStr);
					}
					else if(type.equals(float.class.getName()) || type.equals(double.class.getName()) || type.equals(Float.class.getName()) || type.equals(Double.class.getName())) {
						result = new Double(valueStr);
					}
					else if(type.equals(boolean.class.getName()) || type.equals(Boolean.class.getName())) {
						result = new Boolean(valueStr);
					}
					else if(type.equals(char.class.getName()) || type.equals(Character.class.getName())) {
						if(valueStr.length() == 1) {
							result = new Character(valueStr.charAt(0));
						}
						else {
							throw new Exception("Value " + valueStr + " cannot be converted into a " + type);
						}
					}
					else {
						throw new Exception("Can't handle values of type " + type);
					}
					return result;
				}
				catch(Exception e) {
					throw new Exception("Value " + valueStr + " cannot be converted into a " + type);
				}
			}
			
			public boolean isCellEditable(int row, int col) {
				return (col == 3 && (rows.get(row).getParameter().getMode() == Constants.IN_MODE || 
						             rows.get(row).getParameter().getMode() == Constants.INOUT_MODE));
			}
		};
		
		table = new JTable(model) {

			public TableCellRenderer getCellRenderer(int row, int column) {
				return renderer;
			}

			public TableCellEditor getCellEditor(int row, int col) {
				if (col == 3) {
					return rows.get(row).getValueEditor();
				}
				return null;
			}
		};

		table.setIntercellSpacing(new Dimension(2, 2));
		table.setRowHeight(25);
		labelNameMaxWidth += 10;
		if (labelNameMaxWidth < 150) {
			labelNameMaxWidth = 150;
		}
		table.getColumnModel().getColumn(0).setPreferredWidth(labelNameMaxWidth);
		table.getColumnModel().getColumn(0).setMaxWidth(labelNameMaxWidth);
		table.getColumnModel().getColumn(1).setPreferredWidth(150);
		table.getColumnModel().getColumn(1).setMaxWidth(150);
		table.getColumnModel().getColumn(2).setPreferredWidth(50);
		table.getColumnModel().getColumn(2).setMaxWidth(50);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		
		table.setBackground(getBackground());
		table.setShowGrid(true);
		table.setPreferredScrollableViewportSize(new Dimension(500, 100));
		
		table.getTableHeader().setBackground(Color.LIGHT_GRAY);
		
		setViewportView(table);
	}

	public jade.util.leap.List getParameters(){
		jade.util.leap.List parameters = new jade.util.leap.ArrayList(); 
		for(Row row : rows) {
			parameters.add(row.getParameter());
		}
		return parameters;
	}

	boolean checkInputParameters() {
		boolean valid = true;
		for(Row row : rows) {
			if ((row.getParameter().getMode() == Constants.IN_MODE ||
				row.getParameter().getMode() == Constants.INOUT_MODE) &&
				row.getParameter().getValue() == null) {
				valid = false;
				break;
			}
		}
		return valid;
	}
	
	public void setResult(jade.util.leap.List parameters){
		setParameters(parameters);
	}
	
	private String getParameterMode(int mode){
		String modeLabel;
		if (mode == Constants.IN_MODE){
			modeLabel = Constants.IN_MODE_LABEL;
		}else if (mode == Constants.INOUT_MODE){
			modeLabel = Constants.INOUT_MODE_LABEL;
		}else{
			modeLabel = Constants.OUT_MODE_LABEL;
		}
		return modeLabel;
	}
	
	void setFieldsEnabled(boolean enabled) {
		table.setEnabled(enabled);
	}
	
	void reset() {
		setParameters(new jade.util.leap.ArrayList());
	}	

	private class Row {
		private Parameter parameter;
		private JLabel name;
		private JLabel type;
		private JLabel mode;
		private JComponent valueShower;
		private TableCellEditor valueEditor;

		public Row(Parameter parameter) {
			this.parameter = parameter;
			name = new JLabel(parameter.getName());
			type = new JLabel(parameter.getType());
			mode = new JLabel(getParameterMode(parameter.getMode()), SwingConstants.CENTER);
						
			if (parameter.getType().equals("java.lang.Boolean")) {
				JCheckBox checkBox = new JCheckBox();
				if (parameter.getValue() != null && ((Boolean) parameter.getValue()).booleanValue()) {
					checkBox.setSelected(true);
				}
				checkBox.setEnabled(parameter.getMode() == Constants.IN_MODE || parameter.getMode() == Constants.INOUT_MODE);
				valueEditor = new DefaultCellEditor(checkBox);
				valueShower = checkBox;
			}
			else {
				JTextField textField = new JTextField();
				if (parameter.getValue() != null) {
					textField.setText(parameter.getValue().toString());
				}
				textField.setEditable(parameter.getMode() == Constants.IN_MODE || parameter.getMode() == Constants.INOUT_MODE);
				valueEditor = new DefaultCellEditor(textField);
				valueShower = textField;
			}
			valueShower.setToolTipText("Double click to edit, Enter to validate");
		}

		public void resetValue() {
			if (valueShower instanceof JTextField) {
				((JTextField)valueShower).setText(null);
			}
		}

		public Parameter getParameter() {
			return parameter;
		}
		
		public JComponent getValue() {
			return valueShower;
		}

		public TableCellEditor getValueEditor() {
			return valueEditor;
		}

		public JLabel getName() {
			return name;
		}

		public JLabel getType() {
			return type;
		}

		public JLabel getMode() {
			return mode;
		}
	}
}
