package in.tamchow.sudoku;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.swing.SwingConstants.CENTER;

public class Sudoku extends JFrame {

    private static final String SAVE_FILE_NAME = "Sudoku.sav";

    public Set<Integer> getAllowedValues() {
        return allowedValues;
    }

    @SuppressWarnings("WeakerAccess")
    public void setAllowedValues(Set<Integer> allowedValues) {
        if (allowedValues == null) {
            allowedValues = new LinkedHashSet<>();
        }
        this.allowedValues = allowedValues;
    }

    private static class Cell {
        private final int value;
        private final boolean fixed;

        Cell(int value, boolean fixed) {
            this.value = value;
            this.fixed = fixed;
        }
    }

    private static final long serialVersionUID = 1L;
    private Cell[][] board;
    private int width, height, rows, columns, subGridSize;
    private Set<Integer> allowedValues;
    private double difficulty;
    private int lowerFillLimit, upperFillLimit;
    private Color colorBackgroundA, colorBackgroundB, colorForegroundA, colorForegroundB;
    private JTextField[][] shell;
    private JLabel status;
    private static final int SUBSQUARE_BORDER_WIDTH = 5, CELL_X = 60, CELL_Y = 60;
    private static final Color BORDER_COLOR = Color.BLACK;

    private Sudoku(int rows, int columns) {
        this(rows, columns, -1,
                new Color[]{Color.CYAN, Color.YELLOW, Color.RED, Color.WHITE},
                0.2);
    }

    private static int calculateSubGridSize(int rows, int columns) {
        return (int) Math.sqrt(Math.min(rows, columns));
    }

    private Sudoku(int rows, int columns, int subGridSize, Color[] colors, double difficulty) {
        this.rows = rows > 0 ? rows : 9;
        this.columns = columns > 0 ? columns : 9;
        int minSize = Math.min(this.rows, this.columns);
        int defaultSubGridSize = calculateSubGridSize(this.rows, this.columns);
        this.subGridSize = (subGridSize > 0 && subGridSize <= minSize) ? subGridSize : defaultSubGridSize;
        width = this.rows * (CELL_X + (SUBSQUARE_BORDER_WIDTH / (this.subGridSize - 1)));
        height = this.columns * (CELL_Y + (SUBSQUARE_BORDER_WIDTH / (this.subGridSize - 1)));
        this.difficulty = difficulty < 0.0 ? 0.0 : (difficulty > 1 ? 1.0 : difficulty);
        int maxElementValue = this.subGridSize * this.subGridSize;
        int minElementValue = 1;
        setAllowedValues(IntStream.rangeClosed(minElementValue, maxElementValue)
                .boxed().collect(Collectors.toCollection(LinkedHashSet::new)));
        lowerFillLimit = 2 * Math.max(rows, columns);
        upperFillLimit = (rows * columns) / 2;

        colorBackgroundA = colors[0];
        colorBackgroundB = colors[1];
        colorForegroundA = colors[2];
        colorForegroundB = colors[3];
        if (minSize % this.subGridSize > 0) {
            this.subGridSize = defaultSubGridSize;
        }
        setVisible(false);
        shell = new JTextField[rows][columns];
        for (int i = 0; i < shell.length; i++) {
            for (int j = 0; j < shell[i].length; j++) {
                shell[i][j] = new JTextField();
            }
        }
        initComponents();
        init();
    }

    public static void main(String[] args) {
        final int rows = 9, columns = 9;
        SwingUtilities.invokeLater(() -> new Sudoku(rows, columns));
    }

    private void init() {
        setVisible(false);
        JPanel panel = new JPanel(), superPanel = new JPanel();
        status = new JLabel();
        status.setHorizontalAlignment(CENTER);
        status.setAlignmentY(CENTER_ALIGNMENT);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 18));
        status.setBorder(BorderFactory.createMatteBorder(
                SUBSQUARE_BORDER_WIDTH, SUBSQUARE_BORDER_WIDTH,
                SUBSQUARE_BORDER_WIDTH, SUBSQUARE_BORDER_WIDTH, BORDER_COLOR));
        panel.setSize(width + SUBSQUARE_BORDER_WIDTH * (subGridSize - 1), height + SUBSQUARE_BORDER_WIDTH * (subGridSize - 1));
        panel.setLayout(new GridLayout(rows / subGridSize, columns / subGridSize));
        superPanel.setSize(panel.getWidth(), panel.getHeight() +
                (status.getFont().getSize() + 2 * SUBSQUARE_BORDER_WIDTH));
        superPanel.setLayout(new BorderLayout());
        setSize(superPanel.getWidth(), superPanel.getHeight());
        setTitle("Sudoku");
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        status.setText("Let's start!");
        for (int i = 0; i < rows / subGridSize; ++i) {
            for (int j = 0; j < columns / subGridSize; ++j) {
                JPanel subPanel = new JPanel();
                subPanel.setSize(width / subGridSize, height / subGridSize);
                subPanel.setLayout(new GridLayout(subGridSize, subGridSize));
                subPanel.setBorder(BorderFactory.createMatteBorder(SUBSQUARE_BORDER_WIDTH, SUBSQUARE_BORDER_WIDTH,
                        SUBSQUARE_BORDER_WIDTH, SUBSQUARE_BORDER_WIDTH, BORDER_COLOR));
                for (int k = 0; k < subGridSize; ++k) {
                    for (int l = 0; l < subGridSize; ++l) {
                        int rowIndex = i * (rows / subGridSize) + k, columnIndex = j * (columns / subGridSize) + l;
                        shell[rowIndex][columnIndex] = new JTextField("", 1);
                        subPanel.add(shell[rowIndex][columnIndex]);
                    }
                }
                panel.add(subPanel);
            }
        }
        superPanel.add(panel, BorderLayout.CENTER);
        superPanel.add(status, BorderLayout.SOUTH);
        add(superPanel);
        initSudoku();
        setVisible(true);
    }

    private Random random() {
        return new Random();
    }

    private void initSudoku() {
        board = new Cell[rows][columns];
        int fillLimit = random().nextInt((int) Math.abs(upperFillLimit * difficulty + (1 - difficulty) * lowerFillLimit)) + lowerFillLimit;
        java.util.List<Point> allCoordinates = new ArrayList<>(rows * columns);
        for (int y = 0; y < rows; ++y) {
            for (int x = 0; x < columns; ++x) {
                allCoordinates.add(new Point(x, y));
            }
        }
        Collections.shuffle(allCoordinates);
        java.util.List<Point> probableCoordinates = allCoordinates.subList(0, fillLimit);
        for (int currentlyFilled = 0; currentlyFilled < fillLimit; ++currentlyFilled) {
            Point probableCoordinate = probableCoordinates.get(currentlyFilled);
            int probableRow = (int) probableCoordinate.getX(), probableColumn = (int) probableCoordinate.getY();
            if (board[probableRow][probableColumn] == null) {
                Set<Integer> triedValues = new HashSet<>();
                int value = 0;
                do {
                    if (triedValues.containsAll(allowedValues)) {
                        System.out.println("Recursion, invalid position.");
                        initSudoku();
                    } else {
                        value = randomElement(allowedValues).orElseThrow(() -> new IllegalStateException("Set of allowed values is empty."));
                        triedValues.add(value);
                        System.out.println("Entering value = " + value + " at (" + probableRow + ", " + probableColumn + ").");
                    }
                } while (!valid(board, value, probableRow, probableColumn, true, true));
                board[probableRow][probableColumn] = new Cell(value, true);
            }
        }
        if (countEmptyGrids() > 1) {
            System.out.println("Recursion, more than 1 empty sub-grid.");
            initSudoku();
        }
        Cell[][] boardBackup = new Cell[rows][columns];
        copy(board, boardBackup);
        if (!solve(boardBackup, 0, 0)) {
            System.out.println("Recursion, unsolvable board.");
            initSudoku();
        } else {
            paintSudoku(true);
        }
    }

    /**
     * @param set a Set in which to look for a random element
     * @param <T> generic type of the Set elements
     * @return a random element in the Set or null if the set is empty
     */
    private <T> Optional<T> randomElement(Set<T> set) {
        int size = set.size();
        int itemIndex = new Random().nextInt(size);
        int i = 0;
        for (T element : set) {
            if (i == itemIndex) {
                return Optional.of(element);
            }
            ++i;
        }
        return Optional.empty();
    }

    private void copy(Cell[][] source, Cell[][] destination) {
        for (int rowIndex = 0; rowIndex < source.length && rowIndex < destination.length; ++rowIndex) {
            System.arraycopy(source[rowIndex], 0, destination[rowIndex], 0, Math.min(source[rowIndex].length, destination[rowIndex].length));
        }
    }

    private int countEmptyGrids() {
        int count = 0;
        for (int rowIndex = 0; rowIndex <= rows - subGridSize; rowIndex += subGridSize) {
            for (int columnIndex = 0; columnIndex <= columns - subGridSize; columnIndex += subGridSize) {
                int nonFree = 0;
                for (int gridRowIndex = rowIndex; gridRowIndex < rowIndex + subGridSize; ++gridRowIndex) {
                    for (int gridColumnIndex = columnIndex; gridColumnIndex < columnIndex + subGridSize; ++gridColumnIndex) {
                        if (isLegal(gridRowIndex, gridColumnIndex)) {
                            ++nonFree;
                        }
                    }
                }
                if (nonFree == 0) {
                    ++count;
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder accumulator = new StringBuilder(3 * (rows * columns - calculateFreeCells()));
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < columns; ++columnIndex) {
                if (board[rowIndex][columnIndex] != null && board[rowIndex][columnIndex].value > 0) {
                    accumulator.append(rowIndex).append(columnIndex).append(board[rowIndex][columnIndex]).append(" ");
                }
            }
        }
        return accumulator.toString();
    }

    private int calculateFreeCells() {
        int count = 0;
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < columns; ++columnIndex) {
                if (!isLegal(rowIndex, columnIndex)) {
                    ++count;
                }
            }
        }
        return count;
    }

    private void paintSudoku(boolean self) {
        if (self) {
            for (int rowIndex = 0; rowIndex < shell.length; rowIndex++) {
                for (int columnIndex = 0; columnIndex < shell[rowIndex].length; columnIndex++) {
                    if ((rowIndex * columns + columnIndex) % 2 == 0) {
                        shell[rowIndex][columnIndex].setBackground(colorBackgroundA);
                    } else {
                        shell[rowIndex][columnIndex].setBackground(colorBackgroundB);
                    }
                    shell[rowIndex][columnIndex].setFont(shell[rowIndex][columnIndex].getFont().deriveFont(Font.BOLD, 24));
                    shell[rowIndex][columnIndex].setHorizontalAlignment(CENTER);
                    shell[rowIndex][columnIndex].setAlignmentX(CENTER_ALIGNMENT);
                    if (isLegal(rowIndex, columnIndex)) {
                        if (isFixed(rowIndex, columnIndex)) {
                            shell[rowIndex][columnIndex].setForeground(colorForegroundA);
                            shell[rowIndex][columnIndex].setEditable(false);
                        } else {
                            shell[rowIndex][columnIndex].setForeground(colorForegroundB);
                            shell[rowIndex][columnIndex].setEditable(true);
                        }
                        shell[rowIndex][columnIndex].setText(String.valueOf(board[rowIndex][columnIndex].value));
                    } else {
                        shell[rowIndex][columnIndex].setForeground(colorForegroundB);
                        shell[rowIndex][columnIndex].setText("");
                        shell[rowIndex][columnIndex].setEditable(true);
                    }
                }
            }
        } else {
            for (int rowIndex = 0; rowIndex < shell.length; rowIndex++) {
                for (int columnIndex = 0; columnIndex < shell[rowIndex].length; columnIndex++) {
                    if (isLegal(rowIndex, columnIndex) && shell[rowIndex][columnIndex].getText().equals("")) {
                        shell[rowIndex][columnIndex].setText(String.valueOf(board[rowIndex][columnIndex].value));
                        shell[rowIndex][columnIndex].setEditable(true);
                    } else if (!isLegal(rowIndex, columnIndex)) {
                        shell[rowIndex][columnIndex].setText("");
                    }
                }
            }
        }
    }

    private boolean isLegal(int rowIndex, int columnIndex) {
        return isLegal(board, rowIndex, columnIndex);
    }

    private boolean isLegal(Cell[][] board, int rowIndex, int columnIndex) {
        return board[rowIndex][columnIndex] != null && allowedValues.contains(board[rowIndex][columnIndex].value);
    }

    private boolean verifyNumbers() {
        for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
            for (int columnIndex = 0; columnIndex < columns; ++columnIndex) {
                char input = shell[rowIndex][columnIndex].getText().length() == 1 ? shell[rowIndex][columnIndex].getText().charAt(0) : '\0';
                if (Character.isDigit(input) && (!isFixed(rowIndex, columnIndex))) {
                    board[rowIndex][columnIndex] = new Cell(input - '0', false);
                } else if (!isFixed(rowIndex, columnIndex)) {
                    board[rowIndex][columnIndex] = new Cell(0, false);
                }
                if (!isLegal(rowIndex, columnIndex)) {
                    return false;
                }
            }
        }
        paintSudoku(false);
        return true;
    }

    private boolean isFixed(int rowIndex, int columnIndex) {
        return isFixed(board, rowIndex, columnIndex);
    }

    private static boolean isFixed(Cell[][] board, int rowIndex, int columnIndex) {
        return board[rowIndex][columnIndex] != null && board[rowIndex][columnIndex].fixed;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean valid(Cell[][] board, int rowIndex, int columnIndex, boolean zeroIsValid, boolean countIfZero) {
        return valid(board, board[rowIndex][columnIndex] == null ? 0 : board[rowIndex][columnIndex].value, rowIndex, columnIndex, zeroIsValid, countIfZero);
    }

    @SuppressWarnings("ConstantConditions")
    private boolean valid(Cell[][] board, int value, int rowIndex, int columnIndex, boolean zeroIsValid, boolean countIfZero) {
        if (zeroIsValid && value <= 0) return true;
        int step = subGridSize - 1, gridRowStart, gridRowEnd, gridColumnStart = 0, gridColumnEnd = step;
        outer:
        for (gridRowStart = 0, gridRowEnd = step;
             (gridRowStart < (rows - step)) && (gridRowEnd < rows);
             gridRowStart += subGridSize, gridRowEnd += subGridSize) {
            for (gridColumnStart = 0, gridColumnEnd = step;
                 (gridColumnStart < (columns - step)) && (gridColumnEnd < columns);
                 gridColumnStart += subGridSize, gridColumnEnd += subGridSize) {
                if (rowIndex >= gridRowStart && rowIndex <= gridRowEnd &&
                        columnIndex >= gridColumnStart && columnIndex <= gridColumnEnd) {
                    break outer;
                }
            }
        }
        int rowCount = 0, columnCount = 0, groupCount = 0;
        for (Cell[] row : board) {
            if (row[columnIndex] != null && row[columnIndex].value == value) {
                ++rowCount;
            }
        }
        for (int column = 0; column < columns; ++column) {
            if (board[rowIndex][column] != null && board[rowIndex][column].value == value) {
                ++columnCount;
            }
        }
        for (int i = gridRowStart; i <= gridRowEnd; ++i) {
            for (int j = gridColumnStart; j <= gridColumnEnd; ++j) {
                if (board[i][j] != null && board[i][j].value == value) {
                    ++groupCount;
                }
            }
        }
        return (countIfZero) ?
                rowCount == 0 && columnCount == 0 && groupCount == 0 :
                rowCount == 1 && columnCount == 1 && groupCount == 1;
    }

    private boolean checkBoardValidity(String message, boolean autoSolved) {
        if (verifyNumbers()) {
            int validCount = 0;
            for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
                for (int columnIndex = 0; columnIndex < columns; ++columnIndex) {
                    if (valid(board, rowIndex, columnIndex, false, false)) {
                        ++validCount;
                    } else if (board[rowIndex][columnIndex] != null && board[rowIndex][columnIndex].value != 0) {
                        shell[rowIndex][columnIndex].setBackground(Color.DARK_GRAY);
                    }
                }
            }
            boolean allValid = validCount == (rows * columns);
            if (allValid) {
                status.setBackground(Color.GREEN);
                status.setText(autoSolved ? message : "Congratulations! You have completed the game!");
            } else {
                status.setBackground(Color.ORANGE);
                status.setText("Sorry! Try Again.");
            }
            return allValid;
        } else {
            status.setBackground(Color.RED);
            status.setText("Illegal numbers are present in the grid");
            return false;
        }
    }

    private void save() throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(SAVE_FILE_NAME));
        outputStream.writeObject(this);
        status.setBackground(Color.YELLOW);
        status.setText("The game was saved.");
    }

    private void restore() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(SAVE_FILE_NAME));
        Sudoku save = (Sudoku) inputStream.readObject();
        for (int i = 0; i < this.board.length; i++)
            System.arraycopy(save.board[i], 0, this.board[i], 0, this.board[i].length);
        for (int i = 0; i < this.shell.length; i++)
            System.arraycopy(save.shell[i], 0, this.shell[i], 0, this.shell[i].length);
        paintSudoku(false);
        status.setBackground(Color.YELLOW);
        status.setText("The game was Restored.");
    }

    private void initComponents() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setSize(menuBar.getWidth(), Math.round(getHeight() * .1f));
        JMenu menu = new JMenu();
        menu.setFont(menu.getFont().deriveFont(14.0f));
        JMenuItem newGame = new JMenuItem();
        newGame.setFont(newGame.getFont().deriveFont(14.0f));
        JMenuItem solve = new JMenuItem();
        solve.setFont(solve.getFont().deriveFont(14.0f));
        JMenuItem done = new JMenuItem();
        done.setFont(done.getFont().deriveFont(14.0f));
        JMenuItem save = new JMenuItem();
        save.setFont(save.getFont().deriveFont(14.0f));
        JMenuItem reload = new JMenuItem();
        reload.setFont(reload.getFont().deriveFont(14.0f));
        JMenuItem clearHints = new JMenuItem();
        clearHints.setFont(clearHints.getFont().deriveFont(14.0f));
        JMenuItem exit = new JMenuItem();
        exit.setFont(exit.getFont().deriveFont(14.0f));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        menu.setText("Options");
        newGame.setText("New Game");
        final ActionListener disposer = event -> {
            setVisible(false);
            dispose();
        };
        newGame.addActionListener(event -> {
            disposer.actionPerformed(event);
            new Sudoku(rows, columns);
        });
        newGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        menu.add(newGame);
        clearHints.setText("Clear Hints");
        clearHints.addActionListener(event -> paintSudoku(true));
        clearHints.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        menu.add(clearHints);
        solve.setText("Solve");
        solve.addActionListener(event -> solve(true));
        solve.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menu.add(solve);
        done.setText("Done");
        done.addActionListener(event -> checkBoardValidity("", false));
        done.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        menu.add(done);
        save.setText("Save");
        save.addActionListener(event -> {
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        menu.add(save);
        reload.setText("Restore");
        reload.addActionListener(event -> {
            try {
                restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        reload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        menu.add(reload);
        exit.setText("Exit");
        exit.addActionListener(event -> {
            disposer.actionPerformed(event);
            System.exit(0);
        });
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        menu.add(exit);
        menuBar.add(menu);
        setJMenuBar(menuBar);
        pack();
    }

    @SuppressWarnings("SameParameterValue")
    private boolean solve(boolean log) {
        //printGrid();
        if (solve(board, 0, 0)) {
            if (log) {
                paintSudoku(true);
                checkBoardValidity("Solved", true);
            }
            return true;
        } else {
            if (log) {
                status.setBackground(Color.RED);
                status.setText("No solution");
            }
            return false;
        }
    }

    private boolean solve(Cell[][] board, int i, int j) {
        if (i == rows - 1 && j == columns) {
            return true;
        } else if (j == columns) {
            j = 0;
            i += 1;
        }
        if (board[i][j] != null && (board[i][j].fixed || board[i][j].value > 0)) return solve(board, i, j + 1);
        for (int trialValue : allowedValues) {
            if (valid(board, trialValue, i, j, false, true)) {
                board[i][j] = new Cell(trialValue, false);
                if (solve(board, i, j + 1))
                    return true;
            } else {
                board[i][j] = new Cell(0, false);
            }
        }
        return false;
    }

}