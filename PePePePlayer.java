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
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		mStartCalculation = System.currentTimeMillis();
		mAuthorizedTime = timeout - 5;
		mInitialization = true;
		myRoleIndex = getStateMachine().getRoleIndices().get(getRole());
		roleCount = getStateMachine().getRoles().size();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		mDepthLimit = 10000000;
		Move selection = getMiniMaxMove(moves);
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException
	{
		mStartCalculation = System.currentTimeMillis();
		mInitialization = false;
		mAuthorizedTime = timeout - 5;
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = null;
		if(getStateMachine().getRoles().size()<=2)
		{
			selection = getMiniMaxMove(moves);
		}
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - mStartCalculation));
		return selection;
	}

	public Move getMiniMaxMove(List<Move> legalMoves)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException
	{
		Move selection = legalMoves.get(0);
		System.out.println("getMiniMaxMove : " + legalMoves.size() + " moves possible");
		int miniMaxScore = 0;
		if(legalMoves.size() > 1)
		{
			for( int i=0; i < legalMoves.size(); ++i)
			{
				profondeur = "";
				ArrayList<Move> moveList = new ArrayList<Move>(roleCount);
				movesSelectedCount = 1;
				for(int roleIndex=0; roleIndex<roleCount; ++roleIndex)
				{
					if (roleIndex == myRoleIndex)
					{
						moveList.add(legalMoves.get(i));
					}
					else
					{
						moveList.add(null);
					}
				}
				System.out.println("\n\n getMiniMaxMove : " + legalMoves.get(i).toString());
				int tmpMaxScore = getMinScore(moveList, getCurrentState(), 1, 0, 100, 0);
				System.out.println("getMiniMaxMove : tmpMaxScore=" + tmpMaxScore + " i=" + i + " legalMoves.size()=" + legalMoves.size());
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
				long currentTime = System.currentTimeMillis();
				if(currentTime - mStartCalculation >= mAuthorizedTime)
				{
					break;
				}
			}
		}
		System.out.println("Choosing " + selection.toString() + "\n\n");

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
			System.out.println(profondeur + "getMinScore : " + legalEnemyMoves.get(i).toString() + " " + legalEnemyMoves.size() + " possible moves");
			movesSelectedCount++;
			for(int roleIndex=0; roleIndex<roleCount; ++roleIndex)
			{
				if (playersMoves.get(roleIndex) == null)
				{
					playersMoves.remove(roleIndex);
					playersMoves.add(roleIndex, legalEnemyMoves.get(i));
					break;
				}
			}
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
			playersMoves.remove(1);
			long currentTime = System.currentTimeMillis();
			if(currentTime - mStartCalculation >= mAuthorizedTime)
			{
				break;
			}
		}
		return beta;
	}

	public int getMaxScore(MachineState currentState, int alpha, int beta, int depth)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		if(getStateMachine().isTerminal(currentState))
		{
			int score = getStateMachine().getGoal(currentState, getRole());
			System.out.println(profondeur + "terminal state score : " + score);
			return score;
		}
		//update max depth reached
		if(mInitialization && mMaxDepthReached < depth)
		{
			mMaxDepthReached = depth;
		}
		long currentTime = System.currentTimeMillis();
		if(depth > mDepthLimit || (currentTime - mStartCalculation >= mAuthorizedTime))
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
			System.out.println(profondeur + "getMaxScore : " + legalMoves.get(i).toString() + " " + legalMoves.size() + " possible moves");
			ArrayList<Move> moveList = new ArrayList<Move>();
			moveList.add(legalMoves.get(i));
			profondeur += "  ";
			int tmpMinScore = getMinScore(moveList, currentState, moveList.size(), alpha, beta, depth);
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
			currentTime = System.currentTimeMillis();
			if(currentTime - mStartCalculation >= mAuthorizedTime)
			{
				break;
			}
		}
		return alpha;
	}

	public int evaluateScore(MachineState currentState)
	{
		return 0;
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
