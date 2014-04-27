import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class PePePePlayer extends StateMachineGamer {

	String profondeur;
	int myRoleIndex;
	int roleCount;
	int movesSelectedCount;
	int mDepthLimit;
	int mMyMaxMobility;
	int mMaxEnemyMobility;
	int mMaxDepthReached;
	long mStartCalculation;
	long mAuthorizedTime;
	boolean mInitialization;
	boolean debugMode;
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	public void logDebug(String log)
	{
		if(debugMode)
		{
			System.out.println(log);
		}
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		System.out.println("timeout : " + timeout);
		timeout = 10000;
		mStartCalculation = System.currentTimeMillis();
		System.out.println("timeout : " + timeout);
		mAuthorizedTime = timeout - 500;
		mInitialization = true;
		myRoleIndex = getStateMachine().getRoleIndices().get(getRole());
		roleCount = getStateMachine().getRoles().size();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		mDepthLimit = 1;
		getMiniMaxMove(moves);
		long currentTime = System.currentTimeMillis();
		while(currentTime-mStartCalculation < mAuthorizedTime)
		{
			mDepthLimit++;
			getMiniMaxMove(moves);
			currentTime = System.currentTimeMillis();
		}
		mDepthLimit--;
		debugMode = false;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException
	{
		System.out.println("timeout : " + timeout);
		timeout = 10000;
		mStartCalculation = System.currentTimeMillis();
		mInitialization = false;
		mAuthorizedTime = timeout - 100;
		System.out.println("timeout : " + timeout);
		System.out.println("mMyMaxMobility : " + mMyMaxMobility);
		System.out.println("mMaxEnemyMobility : " + mMaxEnemyMobility);
		System.out.println("mMaxDepthReached : " + mMaxDepthReached);
		System.out.println("mDepthLimit : " + mDepthLimit);
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = null;
		if(getStateMachine().getRoles().size()<=2)
		{
			selection = getMiniMaxMove(moves);
		}
		long stop = System.currentTimeMillis();
		/*
		if(stop - mStartCalculation < mAuthorizedTime/4)
		{
			mDepthLimit++;
		}
		*/
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - mStartCalculation));
		return selection;
	}

	public Move getMiniMaxMove(List<Move> legalMoves)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException
	{
		Move selection = legalMoves.get(0);
		System.out.println("getMiniMaxMove : " + legalMoves.size() + " moves possible");
		int miniMaxScore = 0;
		for( int i=0; i < legalMoves.size(); ++i)
		{
			profondeur = "";
			ArrayList<Move> moveList = new ArrayList<Move>(roleCount);
			movesSelectedCount = 1;
			int nextRoleToFill = -1;
			for(int roleIndex=0; roleIndex<roleCount; ++roleIndex)
			{
				if (roleIndex == myRoleIndex)
				{
					moveList.add(legalMoves.get(i));
				}
				else
				{
					if(nextRoleToFill < 0)
					{
						nextRoleToFill = roleIndex;
					}
					moveList.add(null);
				}
			}
			System.out.println("getMiniMaxMove : test move " + i + "/" + legalMoves.size() + ": " + legalMoves.get(i).toString());
			int tmpMaxScore = getMinScore(moveList, getCurrentState(), nextRoleToFill, 0, 100, 0);
			System.out.println("                 tmpMaxScore=" + tmpMaxScore);
			if( tmpMaxScore == 100)
			{
				selection = legalMoves.get(i);
				break;
			}
			else if(tmpMaxScore > miniMaxScore)
			{
				miniMaxScore = tmpMaxScore;
				selection = legalMoves.get(i);
			}
			if( timeoutReached())
			{
				mDepthLimit--;
				break;
			}
		}
		System.out.println("Choosing " + selection.toString() + " with score " + miniMaxScore + "\n");

		return selection;
	}

	public int getMinScore(List<Move> playersMoves, MachineState currentState, int role, int alpha, int beta, int depth)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		if(roleCount == 1)
		{
			MachineState nextState = getStateMachine().getNextState(currentState, playersMoves);
			profondeur += "  ";
			int score = getMaxScore(nextState, alpha, beta, depth+1);
			profondeur = profondeur.substring(0, profondeur.length()-2);
			return score;
		}
		List<Move> legalEnemyMoves = getStateMachine().getLegalMoves(currentState, getStateMachine().getRoles().get(role));
		// update enemy max mobility
		if(mInitialization && mMaxEnemyMobility < legalEnemyMoves.size())
		{
			 mMaxEnemyMobility = legalEnemyMoves.size();
		}
		for( int i=0; i < legalEnemyMoves.size(); ++i)
		{
			logDebug(profondeur + "getMinScore : " + legalEnemyMoves.get(i).toString() + " " + legalEnemyMoves.size() + " possible moves");
			movesSelectedCount++;
			playersMoves.set(role, legalEnemyMoves.get(i));
			MachineState nextState = getStateMachine().getNextState(currentState, playersMoves);
			profondeur += "  ";
			int tmpMaxScore = getMaxScore(nextState, alpha, beta, depth+1);
			profondeur = profondeur.substring(0, profondeur.length()-2);
			if(tmpMaxScore == 0)
			{
				return 0;
			}
			else if(tmpMaxScore < alpha)
			{
				return alpha;
			}
			else if(tmpMaxScore < beta)
			{
				beta = tmpMaxScore;
			}
			if(timeoutReached())
			{
				break;
			}
		}
		return beta;
	}

	public boolean timeoutReached()
	{
		long currentTime = System.currentTimeMillis();
		if(currentTime - mStartCalculation >= mAuthorizedTime)
		{
			return true;
		}
		return false;
	}

	public int getMaxScore(MachineState currentState, int alpha, int beta, int depth)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		if(getStateMachine().isTerminal(currentState))
		{
			int score = getStateMachine().getGoal(currentState, getRole());
			logDebug(profondeur + "terminal state score : " + score);
			return score;
		}
		//update max depth reached
		if(mInitialization && mMaxDepthReached < depth)
		{
			mMaxDepthReached = depth;
		}
		if(depth > mDepthLimit || timeoutReached())
		{
			return evaluateScore(currentState);
		}

		List<Move> legalMoves = getStateMachine().getLegalMoves(currentState, getRole());
		//update my max mobility
		if(mInitialization && mMyMaxMobility < legalMoves.size())
		{
			mMyMaxMobility = legalMoves.size();
		}

		for( int i=0; i < legalMoves.size(); ++i)
		{
			logDebug(profondeur + "getMaxScore : " + legalMoves.get(i).toString() + " " + legalMoves.size() + " possible moves");
			ArrayList<Move> moveList = new ArrayList<Move>(roleCount);
			movesSelectedCount = 1;
			int nextRoleToFill = -1;
			for(int roleIndex=0; roleIndex<roleCount; ++roleIndex)
			{
				if (roleIndex == myRoleIndex)
				{
					moveList.add(legalMoves.get(i));
				}
				else
				{
					if(nextRoleToFill < 0)
					{
						nextRoleToFill = roleIndex;
					}
					moveList.add(null);
				}
			}
			profondeur += "  ";
			int tmpMinScore = getMinScore(moveList, currentState, nextRoleToFill, alpha, beta, depth);
			profondeur = profondeur.substring(0, profondeur.length()-2);
			if(tmpMinScore == 100)
			{
				return 100;
			}
			else if(tmpMinScore > alpha)
			{
				alpha = tmpMinScore;
			}
			else if(tmpMinScore > beta)
			{
				return beta;
			}
			if( timeoutReached())
			{
				break;
			}
		}
		return alpha;
	}

	public int evaluateScore(MachineState currentState)
			throws MoveDefinitionException
	{
		double score;
		try {
			score = getStateMachine().getGoal(currentState, getRole());
		} catch (GoalDefinitionException e) {
			int legalMovesCount = getStateMachine().getLegalMoves(currentState, getRole()).size();
			if( legalMovesCount > mMyMaxMobility)
			{
				mMyMaxMobility = legalMovesCount;
				score = 100;
			}
			else
			{
				score =  (legalMovesCount * 100) / mMyMaxMobility;
			}
			System.out.println("evaluateScore return " + score);
		}
		if( score == 0)
		{
			score++;
		}
		return (int) score;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

}
