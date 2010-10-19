package com.nullprogram.sudoku;

import java.util.Stack;
import java.util.Random;
import java.util.Collections;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JComponent;

/**
 * A Sudoku board capable of generating puzzles and interacting.
 */
public class Sudoku extends JComponent implements KeyListener, MouseListener {

    private static final long serialVersionUID = 5546778616302943600L;

    private static final float FONT_SIZE = 24f;
    private static final int CELL_SIZE = 40;
    private static final int PADDING = 10;

    /* Work grid and the displayed grid. */
    private byte[][] grid;
    private byte[][] display;
    private byte[][] orig;

    private Random rng;
    private Position origin = new Position((byte) 0, (byte) 0);
    private Stack<Position> positions;
    private Position selected;

    /**
     * Create a new Sudoku board.
     */
    public Sudoku() {
        grid = new byte[9][9];
        display = new byte[9][9];
        orig = new byte[9][9];
        int side = CELL_SIZE * 9 + PADDING * 2;
        Dimension size = new Dimension(side, side);
        setPreferredSize(size);
        setMinimumSize(size);
        setOpaque(true);
        setBackground(Color.white);
        rng = new Random();
        addKeyListener(this);
        addMouseListener(this);
        selected = origin;
    }

    /**
     * Create a new Sudoku puzzle.
     */
    public final void createSudoku() {
        initPositions();
        System.out.println("Generating ...");
        if (generate()) {
            System.out.println("Done.");
        } else {
            System.out.println("Fail.");
        }
        System.out.println("Givens: " + filled());
        copy(grid, orig);
        swap();
        copy(orig, grid);
        repaint();
    }

    /**
     * The main function.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        Sudoku sudoku = new Sudoku();
        JFrame frame = new JFrame("Sudoku");
        frame.add(sudoku);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
        sudoku.createSudoku();
        sudoku.requestFocusInWindow();
    }

    /**
     * Draw a Sudoku board on this component.
     *
     * @param g the graphics to be painted
     */
    public final void paintComponent(final Graphics g) {
        super.paintComponent(g);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        /* Draw marked spaces. */
        g.setColor(Color.LIGHT_GRAY);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int marked = orig[x][y];
                if (marked > 0) {
                    g.fillRect(x * CELL_SIZE + PADDING,
                               y * CELL_SIZE + PADDING,
                               CELL_SIZE, CELL_SIZE);
                }
            }
        }

        /* Draw the grid. */
        g.setColor(Color.BLACK);
        int max = 9 * CELL_SIZE + PADDING + 1;
        for (int i = 0; i <= 9; i++) {
            int d = i * CELL_SIZE + PADDING;
            g.drawLine(d, PADDING, d, max);
            g.drawLine(PADDING, d, max, d);
        }
        for (int i = 0; i <= 9; i += 3) {
            int d = i * CELL_SIZE + PADDING;
            g.drawLine(d - 1, PADDING - 1, d - 1, max);
            g.drawLine(d + 1, PADDING + 1, d + 1, max);
            g.drawLine(PADDING - 1, d - 1, max, d - 1);
            g.drawLine(PADDING + 1, d + 1, max, d + 1);
        }

        /* Draw the selected square. */
        if (selected != null) {
            g.setColor(Color.RED);
            int padding = 3;
            int thickness = 2;
            for (int i = padding; i < padding + thickness; i++) {
                g.drawRect(selected.getX() * CELL_SIZE + PADDING + i,
                           selected.getY() * CELL_SIZE + PADDING + i,
                           CELL_SIZE - i * 2, CELL_SIZE - i * 2);
            }
        }

        /* Set the font. */
        g.setFont(g.getFont().deriveFont(FONT_SIZE));
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        /* Draw the numbers. */
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int val = display[x][y];
                if (val > 0) {
                    int xx = x * CELL_SIZE + PADDING + CELL_SIZE / 2;
                    int yy = y * CELL_SIZE + PADDING + CELL_SIZE / 2;
                    String num = "" + val;
                    g.drawString(num, xx - fm.stringWidth(num) / 2,
                                 yy + fm.getAscent() / 2);
                }
            }
        }
    }

    /**
     * Create a symmetrical order the positions.
     */
    private void initPositions() {
        Stack<Position> tmp = new Stack<Position>();
        positions = new Stack<Position>();
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < y; x++) {
                Position pos = new Position(x, y);
                tmp.push(pos);
            }
        }
        for (byte i = 0; i < 4; i++) {
            tmp.push(new Position(i, i));
        }
        Collections.shuffle(tmp);
        while (!tmp.empty()) {
            Position pos = tmp.pop();
            Position mirror = mirror(pos);
            positions.push(pos);
            positions.push(mirror);
        }
    }

    /**
     * Return mirror of position.
     *
     * @param pos position to mirror
     * @return mirrored position
     */
    private Position mirror(final Position pos) {
        return new Position((byte) (8 - pos.getX()), (byte) (8 - pos.getY()));
    }

    /**
     * Return the number of filled positions.
     *
     * @return number of filled positions
     */
    private int filled() {
        int count = 0;
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                if (grid[x][y] > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get the value at position.
     *
     * @param p position
     * @return the value at the position
     */
    private byte get(final Position p) {
        return grid[p.getX()][p.getY()];
    }

    /**
     * Get the value at position.
     *
     * @param p position
     * @param val the new value
     */
    private void set(final Position p, final byte val) {
        grid[p.getX()][p.getY()] = val;
    }

    /**
     * Reset the position to empty.
     *
     * @param p position to reset.
     */
    private void unset(final Position p) {
        grid[p.getX()][p.getY()] = (byte) 0;
    }

    /**
     * Generate a new Sudoku puzzle.
     *
     * @return true if build was successful
     */
    private boolean generate() {
        Position pos = positions.pop();
        boolean[] possible = possible(pos);
        for (byte i = 0; i < 10; i++) {
            if (possible[i]) {
                set(pos, i);
                int solutions = numSolutions();
                if (solutions > 1) {
                    /* Keep filling in. */
                    if (generate()) {
                        return true;
                    }
                } else if (solutions == 1) {
                    /* Done, exactly one solution left. */
                    return true;
                }
            }
        }
        /* Failed to generate a sudoku from here. */
        unset(pos);
        positions.push(pos);
        return false;
    }

    /**
     * Return the number of solutions on the current board.
     *
     * We only care if this is 0, 1, or greater than 1, so it will
     * never actually return higher than 2.
     *
     * @return number of solutions
     */
    private int numSolutions() {
        Position pos = null;
        for (byte y = 0; pos == null && y < 9; y++) {
            for (byte x = 0; pos == null && x < 9; x++) {
                if (grid[x][y] == 0) {
                    pos = new Position(x, y);
                }
            }
        }
        if (pos == null) {
            /* Board is full i.e. solved. */
            return 1;
        }

        boolean[] possible = possible(pos);
        int count = 0;
        for (byte i = 0; i < 10; i++) {
            if (possible[i]) {
                set(pos, i);
                count += numSolutions();
                if (count > 1) {
                    unset(pos);
                    return 2;
                }
            }
        }
        unset(pos);
        return count;
    }

    /**
     * Possible values for given position.
     *
     * @param pos the position to check
     * @return list of value possibilities
     */
    private boolean[] possible(final Position pos) {
        boolean[] possible = new boolean[10];
        for (int i = 1; i < 10; i++) {
            possible[i] = true;
        }
        for (int x = 0; x < 9; x++) {
            possible[grid[x][pos.getY()]] = false;
        }
        for (int y = 0; y < 9; y++) {
            possible[grid[pos.getX()][y]] = false;
        }
        int xx = (pos.getX() / 3) * 3;
        int yy = (pos.getY() / 3) * 3;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                possible[grid[xx + x][yy + y]] = false;
            }
        }
        possible[0] = false;
        return possible;
    }

    /**
     * Swap the display and working grids.
     *
     * This is so the display is always working with a consistent grid.
     */
    private void swap() {
        byte[][] tmp = grid;
        grid = display;
        display = tmp;
    }

    /**
     * Copy one grid to another.
     *
     * @param from origin grid
     * @param to destination grid
     */
    private void copy(final byte[][] from, final byte[][] to) {
        for (byte y = 0; y < 9; y++) {
            for (byte x = 0; x < 9; x++) {
                to[x][y] = from[x][y];
            }
        }
    }

    /**
     * Move the selected position in a direction.
     *
     * @param xDiff change in x-direction
     * @param yDiff change in y-direction
     */
    private void moveSelected(final int xDiff, final int yDiff) {
        if (selected != null) {
            byte x = (byte) (selected.getX() + xDiff);
            byte y = (byte) (selected.getY() + yDiff);
            setSelected(new Position(x, y));
        }
    }

    /**
     * Set the selected position to this position, if it is valid.
     *
     * @param pos position to set
     * @return true if position was selected
     */
    private boolean setSelected(final Position pos) {
        byte x = pos.getX();
        byte y = pos.getY();
        if ((x < 9) && (x >= 0) && (y < 9) && (y >= 0)) {
            selected = pos;
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    public final void keyTyped(final KeyEvent e) {
        char c = e.getKeyChar();
        if ((selected != null) && (c >= 49) && (c <= 57)) {
            /* Number 1..9 */
            int x = selected.getX();
            int y = selected.getY();
            if (orig[x][y] == 0) {
                display[x][y] = (byte) (c - 48);
            }
        }
        repaint();
    }

    /** {@inheritDoc} */
    public final void keyPressed(final KeyEvent e) {
        if (e.isActionKey()) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                moveSelected(-1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                moveSelected(1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                moveSelected(0, -1);
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                moveSelected(0, 1);
            }
        }
        repaint();
    }

    /** {@inheritDoc} */
    public void keyReleased(final KeyEvent e) {
        /* Do nothing. */
    }

    /** {@inheritDoc} */
    public final void mouseReleased(final MouseEvent e) {
        int px = (int) e.getPoint().getX();
        int py = (int) e.getPoint().getY();
        int x = (px - PADDING) / CELL_SIZE;
        int y = (py - PADDING) / CELL_SIZE;
        if (!setSelected(new Position((byte) x, (byte) y))) {
            selected = null;
        }
        repaint();
    }

    /** {@inheritDoc} */
    public final void mouseClicked(final MouseEvent e) {
        requestFocusInWindow();
    }

    /** {@inheritDoc} */
    public void mouseExited(final MouseEvent e) {
        /* Do nothing */
    }

    /** {@inheritDoc} */
    public void mouseEntered(final MouseEvent e) {
        /* Do nothing */
    }

    /** {@inheritDoc} */
    public void mousePressed(final MouseEvent e) {
        /* Do nothing */
    }
}
