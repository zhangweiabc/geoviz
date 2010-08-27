package geovista.geoviz.spreadsheet;

/*
 * Description:
 * Date: Feb 9, 2003
 * Time: 12:09:50 PM
 * @author Jin Chen
 */

import javax.swing.table.DefaultTableModel;

import geovista.common.data.DataSetForApps;

public class TableViewerModel extends DefaultTableModel {

	DataSetForApps dataSet;

	@Override
	public int getColumnCount() {
		if (dataSet == null) {
			return 0;
		}

		int nCols = dataSet.getAttributeNamesOriginal().length;
		// if (dataSet.getShapeData() != null) {
		nCols = nCols + 1;
		// }
		return nCols;

	}

	@Override
	public int getRowCount() {
		if (dataSet == null) {
			return 0;
		}
		return dataSet.getNumObservations();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// we are going to be fancy and treat the geom column
		// as the first column
		boolean haveGeom = (dataSet.getShapeData() != null);

		if (columnIndex == 0 && haveGeom) {
			return dataSet.getShapeData()[rowIndex];
		} else if (columnIndex == 0) {
			return null;
		}

		return dataSet.getValueAt(rowIndex, columnIndex - 1);
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex == 0) {
			return "Geometry";
		}
		return dataSet.getAttributeNamesOriginal()[columnIndex - 1];
	}

	void setDataSet(DataSetForApps data) {
		dataSet = data;
		super.addColumn("Geometry", data.getShapeData());
		for (int i = 1; i < data.getAttributeNamesOriginal().length + 1; i++) {
			Object[] namedArrays = data.getNamedArrays();

			super.addColumn(data.getAttributeNamesOriginal()[i - 1],
					(Object[]) namedArrays[i - 1]);
		}

		super.setRowCount(getRowCount());
		super.setColumnCount(getColumnCount());

		fireTableStructureChanged();
	}
}
