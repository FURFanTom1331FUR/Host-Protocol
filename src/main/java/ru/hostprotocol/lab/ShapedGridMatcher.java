package ru.hostprotocol.lab;

/**
 * Places a shaped recipe anywhere inside a larger grid. Extra filled cells fail the match.
 */
public final class ShapedGridMatcher {
	private ShapedGridMatcher() {}

	public static boolean isEmpty(String cell) {
		return cell == null || cell.isEmpty();
	}

	public static boolean matches(String[][] grid, String[][] pattern) {
		if (grid == null || pattern == null || grid.length == 0 || pattern.length == 0) {
			return false;
		}
		int gh = grid.length;
		int gw = grid[0].length;
		int ph = pattern.length;
		int pw = pattern[0].length;
		if (ph > gh || pw > gw) {
			return false;
		}
		for (int row = 0; row <= gh - ph; row++) {
			for (int col = 0; col <= gw - pw; col++) {
				if (matchesAt(grid, pattern, row, col)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean matchesAt(String[][] grid, String[][] pattern, int offRow, int offCol) {
		int gh = grid.length;
		int gw = grid[0].length;
		int ph = pattern.length;
		int pw = pattern[0].length;
		for (int r = 0; r < gh; r++) {
			for (int c = 0; c < gw; c++) {
				boolean inside = r >= offRow && r < offRow + ph && c >= offCol && c < offCol + pw;
				String cell = grid[r][c];
				if (inside) {
					String want = pattern[r - offRow][c - offCol];
					if (isEmpty(want)) {
						if (!isEmpty(cell)) {
							return false;
						}
					} else if (!want.equals(cell)) {
						return false;
					}
				} else if (!isEmpty(cell)) {
					return false;
				}
			}
		}
		return true;
	}
}
