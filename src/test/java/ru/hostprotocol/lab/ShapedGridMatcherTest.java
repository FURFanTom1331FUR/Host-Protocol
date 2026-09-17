package ru.hostprotocol.lab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapedGridMatcherTest {
	private static final String[][] MK2 = LabRecipes.PDA_MK2_PATTERN;

	@Test
	void matchesCenteredInFiveByFive() {
		String[][] grid = empty(5, 5);
		place(grid, 1, 1, MK2);
		assertTrue(ShapedGridMatcher.matches(grid, MK2));
	}

	@Test
	void matchesTopLeftAndBottomRight() {
		String[][] topLeft = empty(5, 5);
		place(topLeft, 0, 0, MK2);
		assertTrue(ShapedGridMatcher.matches(topLeft, MK2));

		String[][] bottomRight = empty(5, 5);
		place(bottomRight, 2, 2, MK2);
		assertTrue(ShapedGridMatcher.matches(bottomRight, MK2));
	}

	@Test
	void extraItemFails() {
		String[][] grid = empty(5, 5);
		place(grid, 1, 1, MK2);
		grid[0][0] = "minecraft:dirt";
		assertFalse(ShapedGridMatcher.matches(grid, MK2));
	}

	@Test
	void emptyGridFails() {
		assertFalse(ShapedGridMatcher.matches(empty(5, 5), MK2));
	}

	@Test
	void wrongCenterFails() {
		String[][] grid = empty(5, 5);
		place(grid, 1, 1, MK2);
		grid[2][2] = "minecraft:glass";
		assertFalse(ShapedGridMatcher.matches(grid, MK2));
	}

	private static String[][] empty(int h, int w) {
		return new String[h][w];
	}

	private static void place(String[][] grid, int row, int col, String[][] pattern) {
		for (int r = 0; r < pattern.length; r++) {
			System.arraycopy(pattern[r], 0, grid[row + r], col, pattern[r].length);
		}
	}
}
