package edu.pdx.cs.cs554.gomoku;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class Board extends View {
    protected int numColumns, numRows;
    protected int cellWidth, cellHeight;
    protected boolean hasWinner = false;
    protected GameType gameType = GameType.STANDARD;
    private GameMode gameMode = GameMode.OFFLINE;
    private Paint blackPaint = new Paint();
    private Paint whitePaint = new Paint();
    public static String[][] cellChecked;
    private Player blackPlayer;
    private Player whitePlayer;
    private Player activePlayer = blackPlayer;
    private AI AIMode;

    public int numStonesPlaced = 0;

    public Board(Context context) {
        this(context, null);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        blackPaint.setStrokeWidth(8);
        whitePaint.setColor(Color.WHITE);
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        calculateDimensions();
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
        Log.i("INFO", "Game type is set to " + this.gameType);
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
        Log.i("INFO", "Game mode is set to " + this.gameMode);


        if (gameMode.equals(GameMode.AI)) {
            Log.i("INFO", "AI MODE");
            AIMode = new AI(numRows, numColumns);
        }
    }

    public Player getBlackPlayer() {
        return this.blackPlayer;
    }

    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public Player getWhitePlayer() {
        return this.whitePlayer;
    }

    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public void setActivePlayer(Player activePlayer) {
        this.activePlayer = activePlayer;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }

        cellWidth = getWidth() / (numColumns + 1);
        cellHeight = cellWidth;

        cellChecked = new String[numColumns][numRows];
        blockEdgeBoard();
        invalidate();
    }

    //Put invisible stones on the edge of the board
    //So black and white cannot be placed on board edge
    private void blockEdgeBoard() {
        for(int column = 0; column < numColumns; column++) {
            //Log.i("INFO", column + "," + 0);
            cellChecked[column][0] = "BLANK";
            cellChecked[column][numRows-1] = "BLANK";
        }

        for(int row = 0; row < numRows; row++) {
            cellChecked[0][row] = "BLANK";
            cellChecked[numColumns-1][row] = "BLANK";
        }
    }

    //=========CHECK STALEMATE =========
    private boolean checkStaleMate() {
        int numStonesSlots = (numColumns-2) * (numRows-2);
        if(numStonesPlaced == (numStonesSlots)) {
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_black)).pause();
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_white)).pause();
            String msg = "Stalemate. It's a tie!";
            showWinningMessage(msg);
            return true;
        }
        return false;
    }

    //=========CHECK WINNER=============
    private boolean findWinner() {
        boolean blackWins = checkHorizontal("BLACK") || checkVertical("BLACK") ||
            checkLeftDiagonal("BLACK") || checkRightDiagonal("BLACK");
        boolean whiteWins =  checkHorizontal("WHITE") || checkVertical("WHITE") ||
            checkLeftDiagonal("WHITE") || checkRightDiagonal("WHITE");

        hasWinner = blackWins || whiteWins;
        if (hasWinner) {
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_black)).pause();
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_white)).pause();

            String msg = (blackWins ? "Black" : "White") + " player wins!  Click BACK TO MENU.";
            showWinningMessage(msg);
            SharedPreferences.Editor editor = ((MainActivity) getContext())
                    .getPreferences(Context.MODE_PRIVATE).edit();
            if (gameMode.equals(GameMode.AI)) {
                if (blackWins) {    // human wins
                    blackPlayer.incrementScore();
                    editor.putInt(blackPlayer.getName(), blackPlayer.getScore());
                    editor.commit();
                }
            } else if (gameMode.equals(GameMode.OFFLINE)) {
                Player winner = blackWins ? blackPlayer : whitePlayer;
                Player loser = blackWins ? whitePlayer : blackPlayer;
                winner.incrementScore();
                editor.putInt(winner.getName(), winner.getScore());
                editor.putInt(loser.getName(), loser.getScore());
                editor.commit();
            }
        }

        return hasWinner;
    }

    void showWinningMessage(String msg) {
        TextView winnerMessage = (TextView) ((MainActivity) getContext()).findViewById(R.id.winner_message);
        winnerMessage.setText(msg);
        winnerMessage.setBackgroundColor(Color.WHITE);
        winnerMessage.setVisibility(View.VISIBLE);
    }

    //Check if the end is blocked
    private boolean isNotBlockedEnd(int column, int row, String playerColor) {
        return (cellChecked[column][row]) == playerColor;
    }

    private boolean isNotBlockedEnd(int column, int row) {
        return (cellChecked[column][row]) == null;
    }

    //Find Winner by doing horizontal check.
    //Return true if found a hasWinner
    private boolean checkHorizontal(String playerColor) {
        boolean isWinner = false;
        for (int row = 0; row < numRows; row++) {
            int score = 0;
            for (int column = 0; column < numColumns; column++) {

                if (cellChecked[column][row] == playerColor && score < 5) {
                    score++;

                    //Log.i("INFO", "SCORE:" + score);
                    //Found 5 in a row

                    if (score == 5) {

                        //CHECK if there's NO 6 in a row AND
                        // (left ends is NULL OR right ends is NULL)
                        // checks XOOOOO   or  OOOOOX or OOOOO
                        if (!isNotBlockedEnd(column+1, row, playerColor) &&
                                (isNotBlockedEnd(column-5, row) || isNotBlockedEnd(column+1, row))) {
                            Log.i("INFO", playerColor + " IS THE WINNER");
                            isWinner = true;
                            break;
                        }

                        // checks OOOOOO (6x) or more
                        if (gameType.equals(GameType.FREESTYLE) && isNotBlockedEnd(column+1, row, playerColor)) {
                            Log.i("INFO", playerColor + " IS THE WINNER in freestyle game type");
                            isWinner = true;
                            break;
                        }

                        //Blocked end at both side
                        isWinner = false;
                        score = 0;
                    }
                    continue;
                }
                isWinner = false;
                score = 0;
            }

            if (isWinner)
                break;
        }
        return isWinner;
    }

    private boolean checkVertical(String playerColor) {
        boolean isWinner = false;
        for (int column = 0; column < numColumns; column++) {
            int score = 0;
            for (int row = 0; row < numRows; row++) {

                if (cellChecked[column][row] == playerColor && score < 5) {
                    score++;

                    //Log.i("INFO", "SCORE:" + score);
                    //Found 5 in a row

                    if (score == 5) {

                        //CHECK if there's NO 6 in a row AND
                        // (left ends is NULL OR right ends is NULL)
                        // checks XOOOOO   or  OOOOOX or OOOOO
                        if (!isNotBlockedEnd(column, row+1, playerColor) &&
                                (isNotBlockedEnd(column, row-5) || isNotBlockedEnd(column, row+1))) {
                            Log.i("INFO", playerColor + " IS THE WINNER");
                            isWinner = true;
                            break;
                        }

                        // checks OOOOOO (6x) or more
                        if (gameType.equals(GameType.FREESTYLE) && isNotBlockedEnd(column, row+1, playerColor)) {
                            Log.i("INFO", playerColor + " IS THE WINNER in freestyle game type");
                            isWinner = true;
                            break;
                        }

                        //Blocked end at both side
                        isWinner = false;
                        score = 0;
                    }
                    continue;
                }
                isWinner = false;
                score = 0;
            }

            if (isWinner)
                break;
        }
        return isWinner;
    }

    //Check right diagonal ↗↗↗↗↗↗
    private boolean checkRightDiagonal(String playerColor) {
        boolean isWinner = false;
        for( int k = 0 ; k < numColumns * 2 ; k++ ) {
            int score = 0;
            for( int column = 0 ; column <= k ; column++ ) {
                int row = k - column;
                if ( row < numColumns && column < numColumns ) {
                    //cellChecked[column][row] = "BLACK";
                    if (cellChecked[column][row] == playerColor && score < 5) {
                        score++;

                        if (score == 5) {
                            //CHECK if there's NO 6 in a row AND
                            // (left ends is NULL OR right ends is NULL)
                            // checks XOOOOO   or  OOOOOX or OOOOO
                            if (!isNotBlockedEnd(column+1, row+1, playerColor) &&
                                    (isNotBlockedEnd(column-5, row+5) || isNotBlockedEnd(column+1, row+1))) {
                                Log.i("INFO", playerColor + " IS THE WINNER");
                                isWinner = true;
                                break;
                            }

                            // checks OOOOOO (6x) or more

                            if (gameType.equals(GameType.FREESTYLE) && isNotBlockedEnd(column+1, row-1, playerColor)) {
                                Log.i("INFO", playerColor + " IS THE WINNER in freestyle game type");
                                isWinner = true;
                                break;
                            }

                            //Blocked end at both side
                            isWinner = false;
                            score = 0;
                        }
                        continue;
                    }
                    isWinner = false;
                    //Log.i("INFO", column + "," + row);
                    score = 0;

                }
            }
            if (isWinner)
                break;
        }
        return isWinner;
    }

    private boolean checkLeftDiagonal(String playerColor) {
        int score = 0;
        boolean isWinner = false;

        //number of reverse diagonal
        int k = numRows + numColumns - 1;
        int row = numRows - k;
        for(int i =numRows-1; i>=row; i--) {
            int tmpRow = i;
            int tmpCol= 0;
            while(tmpRow<numRows && tmpCol<numColumns) {
                if (tmpRow<0) {
                    tmpCol++;
                    tmpRow++;
                } else {
                    if (cellChecked[tmpCol][tmpRow] == playerColor && score < 5) {
                        score++;
                        //Log.i("INFO", "SCORE: " + score);
                        if (score == 5) {
                            //CHECK if there's NO 6 in a row AND
                            // (left ends is NULL OR right ends is NULL)
                            // checks XOOOOO   or  OOOOOX or OOOOO
                            if (!isNotBlockedEnd(tmpCol+1, tmpRow+1, playerColor) &&
                                    (isNotBlockedEnd(tmpCol-5, tmpRow-5) || isNotBlockedEnd(tmpCol+1, tmpRow+1))) {
                                Log.i("INFO", playerColor + " IS THE WINNER");
                                isWinner = true;
                                break;
                            }

                            // checks OOOOOO (6x) or more
                            if (gameType.equals(GameType.FREESTYLE) && isNotBlockedEnd(tmpCol+1, tmpRow+1, playerColor)) {
                                Log.i("INFO", playerColor + " IS THE WINNER in freestyle game type");
                                isWinner = true;
                                break;
                            }
                            score = 0;
                            isWinner = false;
                        }
                    } else {
                        score = 0;
                        isWinner = false;
                    }

                    //Log.i("INFO", tmpCol + "," + tmpRow);
                    tmpCol++;
                    tmpRow++;

                }
            }
            if (isWinner)
                break;
            score = 0;
            //Log.i("INFO", "SCORE: " + score);
        }
        return isWinner;
    }

    private void drawBoard(Canvas canvas) {
        if (numColumns == 0 || numRows == 0) {
            return;
        }

        for (int i = 1; i < numColumns + 1; i++) {
            canvas.drawLine(i * cellWidth, cellHeight, i * cellWidth, numRows * cellHeight, blackPaint);
            canvas.drawLine(cellWidth, i * cellHeight, numRows * cellWidth, i * cellHeight, blackPaint);
        }
    }

    private void drawStone(Canvas canvas) {
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                if (cellChecked[i][j] != null) {
                    if (cellChecked[i][j] == "WHITE") {
                        canvas.drawCircle((i+1) *cellWidth, (j+1)*cellHeight, cellWidth/3, whitePaint);
                    } else if (cellChecked[i][j] == "BLACK") {
                        canvas.drawCircle((i+1)*cellWidth, (j+1)*cellHeight, cellWidth/3, blackPaint);
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        drawBoard(canvas);
        drawStone(canvas);
        findWinner();
        checkStaleMate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            //This block of code will round up the to the board intersection position.
            float xPosition = event.getX()/cellWidth;
            float yPosition = event.getY()/cellHeight;
            int column = Math.round(xPosition) -1;
            int row = Math.round(yPosition)-1;


            //If position is out of the grid
            if (column < 0 || row < 0) {
                return false;
            }

            if (column >= numColumns || row >= numRows) {
                return false;
            }

            if (hasWinner)
                return false;

            //If position is already placed by other stone
            if (cellChecked[column][row] != null) {
                return false;
            }

            if (gameMode.equals(GameMode.OFFLINE)) {
                OfflineMode(column, row);
            } else if (gameMode.equals(GameMode.AI)) {
                AIMode(column, row);
            }
            Log.i("INFO", "NUMBER OF STONES PLACED: " + numStonesPlaced + "/" + (numRows-2)*(numColumns-2));
            invalidate();
        }
        return true;
    }

    private void OfflineMode(int column, int row) {
        if (activePlayer.isWhite()) {
            cellChecked[column][row] = "WHITE";
            activePlayer = blackPlayer;
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_white)).pause();
            if (!hasWinner) {
                ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_black)).start();
            }
        } else {
            cellChecked[column][row] = "BLACK";
            activePlayer = whitePlayer;
            ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_black)).pause();
            if (!hasWinner) {
                ((TimerView) ((MainActivity) getContext()).findViewById(R.id.timer_white)).start();
            }
        }
        Log.i("INFO", cellChecked[column][row] + ": "+ column + " , " + row);
        numStonesPlaced++;
    }

    private void AIMode(int column, int row) {
        cellChecked[column][row] = "BLACK";     // in AI Mode the human player is always black
        Log.i("INFO", cellChecked[column][row] + ": "+ column + " , " + row);
        if (findWinner()) {
            return;
        }
        AIMode.computerMove(column, row);
        numStonesPlaced += 2;
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
