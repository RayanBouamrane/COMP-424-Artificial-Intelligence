package student_player;

import java.util.ArrayList;

import boardgame.Move;

import pentago_twist.PentagoPlayer;
import pentago_twist.PentagoBoardState.Piece;
import pentago_twist.PentagoBoardState;
import pentago_twist.PentagoMove;

/**
 * A player file submitted by a student.
 */
public class StudentPlayer extends PentagoPlayer {

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260788250");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */

    private PentagoMove bestMove;

    // Fields used to store time taken for computation.
    private long start;
    private long end;

    public Move chooseMove(PentagoBoardState boardState) {

        start = System.currentTimeMillis();

        alphaBeta(boardState, 3, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);

        end = System.currentTimeMillis();

        // If an illegal move is returned, to avoid forfeiting
        // we return a random legal move.
        if (boardState.isLegal(bestMove)) { return bestMove; } else { return boardState.getRandomMove(); }

    }

    public double alphaBeta(PentagoBoardState boardState, int depth, double a, double b, boolean maximizingPlayer) {

        end = System.currentTimeMillis();

        // If depth = 0, or we have a winning/losing game state
        // we return the evaluation of the position.
        // If we are too close to using up 2 seconds, we return the best move
        // we have calculated so far.
        if (depth == 0 || boardState.gameOver() || (end - start >= 1800)) { return evaluation(boardState); }

        ArrayList<PentagoMove> allLegalMoves = boardState.getAllLegalMoves();

        double value;
        // Implementation of alpha-beta pruning.
        if (maximizingPlayer) {
            value = Double.NEGATIVE_INFINITY;

            // For all legal moves, we recursively call alphaBeta, switching the
            // player's turn and decreasing depth by 1 each level.
            for (PentagoMove move : allLegalMoves) {
                PentagoBoardState boardStateClone = (PentagoBoardState) boardState.clone();
                boardStateClone.processMove(move);

                value = Math.max(value, alphaBeta(boardStateClone, depth - 1, a, b, false));

                // If we are at the top level and we are going to increase alpha,
                // we have a better move possible and update bestMove.
                if (depth == 3 && value > a) { bestMove = move; }

                a = Math.max(a, value);

                // Prune when alpha >= beta.
                if (a >= b) { break; }
            }

        } else {
            value = Double.POSITIVE_INFINITY;

            for (PentagoMove move : allLegalMoves) {
                PentagoBoardState boardStateClone = (PentagoBoardState) boardState.clone();
                boardStateClone.processMove(move);

                value = Math.min(value, alphaBeta(boardStateClone, depth - 1, a, b, true));

                b = Math.min(b, value);

                if (b <= a) { break; }
            }
        }
        return value;
    }

    // Evaluates the current board.
    public double evaluation(PentagoBoardState boardState) {

        // If the game state is a win, return positive infinity
        // as the evaluation. This outcome is the best possible.
        // No need to evaluate further.
        if (player_id == boardState.getWinner()) { return Double.POSITIVE_INFINITY; }

        // If the winner is the opponent, return negative infinity,
        // this particular move is clearly losing.
        if (1 - player_id == boardState.getWinner()) { return Double.NEGATIVE_INFINITY; }

        // Our evaluation counts how many open connected marbles we have,
        // subtracts how many the opponent has and adds some score based
        // on how advantageous the position looks (doesn't directly analyse further).
        // If player is white, we calculate black first so that our fields are correctly
        // updated when we call positionalAdv().

        if (player_id == 0) {
            return -countHorizVert(boardState, Piece.BLACK, true) + countHorizVert(boardState, Piece.BLACK, false) + countDiags(boardState, Piece.BLACK)
                    + countHorizVert(boardState, Piece.WHITE, true) + countHorizVert(boardState, Piece.WHITE, false) + countDiags(boardState, Piece.WHITE)
                    + positionalAdv();

        } else {
            return -countHorizVert(boardState, Piece.WHITE, true) + countHorizVert(boardState, Piece.WHITE, false) + countDiags(boardState, Piece.WHITE)
                    + countHorizVert(boardState, Piece.BLACK, true) + countHorizVert(boardState, Piece.BLACK, false) + countDiags(boardState, Piece.BLACK)
                    + positionalAdv();
        }
    }

    // These variables store the length of the longest open line
    // in each of the 3 directions.
    // An open line is a row, column, or diagonal with only
    // our colour of marble along it.
    private int maxHoriz = 0;
    private int maxVert = 0;
    private int maxDiags = 0;

    // Here we count marbles on open rows or columns.
    // The way to count rows is very similar to how we count columns.
    // The flag isHoriz denotes direction.
    public int countHorizVert(PentagoBoardState boardState, Piece pColour, boolean isHoriz) {

        Piece oColour = Piece.values()[1 - pColour.ordinal()];

        int count = 0;
        int temp = 0;
        int maxTemp = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                temp = 0;
                if (isHoriz) {

                    // We increment if we see another marble of our colour.
                    if (boardState.getPieceAt(i, j).equals(pColour)) {
                        temp++;
                    }
                    // If we see an opposing marble, the line is closed to us.
                    // This is disadvantageous, and lowers the evaluation score.
                    if (boardState.getPieceAt(i, j).equals(oColour)) {
                        temp = 0;
                        break;
                    }
                } else {

                    if (boardState.getPieceAt(j, i).equals(pColour)) {
                        temp++;
                    }
                    if (boardState.getPieceAt(j, i).equals(oColour)) {
                        temp = 0;
                        break;
                    }
                }
            }
            // We store the fullest open line we found, and store it globally.
            maxTemp = Math.max(temp, maxTemp);
            count += temp;
        }
        if (isHoriz) { maxHoriz = maxTemp; } else { maxVert = maxTemp; }

        return count;
    }

    public int countDiags(PentagoBoardState boardState, Piece pColour) {

        // There are a total of 6 diagonals where it is possible
        // to have 5 marbles in a row.
        Piece oColour = Piece.values()[1 - pColour.ordinal()];

        int count = 0;
        int temp = 0;
        int maxTemp = 0;
        for (int i = 0; i < 6; i++) {
            if (boardState.getPieceAt(i, i).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, i).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        temp = 0;
        for (int i = 0; i < 6; i++) {
            if (boardState.getPieceAt(i, 5 - i).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, 5 - i).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        temp = 0;
        for (int i = 1; i < 6; i++) {
            if (boardState.getPieceAt(i, i - 1).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, i - 1).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        temp = 0;
        for (int i = 0; i < 5; i++) {
            if (boardState.getPieceAt(i, 4 - i).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, 4 - i).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        temp = 0;
        for (int i = 0; i < 5; i++) {
            if (boardState.getPieceAt(i, i + 1).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, i + 1).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        temp = 0;
        for (int i = 1; i < 6; i++) {
            if (boardState.getPieceAt(i, 6 - i).equals(pColour)) { temp++; }
            if (boardState.getPieceAt(i, 6 - i).equals(oColour)) {
                temp = 0;
                break;
            }
        }

        maxTemp = Math.max(temp, maxTemp);
        count += temp;
        maxDiags = maxTemp;

        return count;
    }

    public double positionalAdv() {

        int diag = 0;
        int hor = 0;
        int ver = 0;

        // We check if we have combinations of long
        // diagonal, vertical, and horizontal lines.

        if (maxDiags >= 3) { diag = 1; }
        if (maxHoriz >= 3) { hor = 1; }
        if (maxVert >= 3) { ver = 1; }

        // we return the number of combinations
        // of open lines we have
        return diag + hor + ver;

    }
}