

import java.util.Arrays;
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

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = null;
		if(getStateMachine().getRoles().size()==1)
		{
			selection = getMoveAccordingCompulsiveDeliberation(moves);
		}
		else if(getStateMachine().getRoles().size()==2)
		{
			selection = getMiniMaxMove(moves);
		}
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	/*
	 * Parcours l'arbre du jeu en profondeur et renvoi le mouvement correspondant au score maximal qui peut être obtenu à la fin
	 * Ne marche que pour les jeux à un seul joueur et qui dont l'arbre peut être entièrement parcouru en 100ms
	public Move getMoveAccordingCompulsiveDeliberation(List<Move> legalMoves)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException
	{
		Move selection = legalMoves.get(0);
		int maxScore = 0;
		for( int i=0; i < legalMoves.size(); ++i)
		{
			int tmpMaxScore = getMaxScore(legalMoves.get(i), getCurrentState());
			if( tmpMaxScore == 100)
			{
				selection = legalMoves.get(i);
				break;
			}
			else if(tmpMaxScore > maxScore)
			{
				maxScore = tmpMaxScore;
				selection = legalMoves.get(i);
			}
		}
		return selection;
	}
	 */

	public Move getMiniMaxMove(List<Move> legalMoves)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException
	{
		Move selection = legalMoves.get(0);
		int miniMaxScore = 0;
		for( int i=0; i < legalMoves.size(); ++i)
		{
			List<Move> moveList = Arrays.asList(legalMoves.get(i));
			int tmpMaxScore = getMinScore(moveList, getCurrentState(), 1);
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
		}
		return selection;
	}

	public int getMinScore(List<Move> playersMoves, MachineState currentState, int role)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		int minScore = 0;
		List<Move> legalEnemyMoves = getStateMachine().getLegalMoves(currentState, getStateMachine().getRoles().get(role));
		for( int i=0; i < legalEnemyMoves.size(); ++i)
		{
			playersMoves.add(legalEnemyMoves.get(i));
			MachineState nextState = getStateMachine().getNextState(currentState, playersMoves);
			int tmpMinScore = getMaxScore(nextState);
			if(tmpMinScore == 0)
			{
				return 0;
			}
			else if(tmpMinScore < minScore)
			{
				minScore = tmpMinScore;
			}
		}
		return minScore;
	}

	public int getMaxScore(MachineState currentState)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		if(getStateMachine().isTerminal(currentState))
		{
			return getStateMachine().getGoal(currentState, getRole());
		}

		int maxScore = 0;
		List<Move> legalMoves = getStateMachine().getLegalMoves(currentState, getRole());
		for( int i=0; i < legalMoves.size(); ++i)
		{
			List<Move> moveList = Arrays.asList(legalMoves.get(i));
			int tmpMaxScore = getMinScore(moveList, currentState, moveList.size());
			if(tmpMaxScore == 100)
			{
				return 100;
			}
			else if(tmpMaxScore > maxScore)
			{
				maxScore = tmpMaxScore;
			}
		}
		return maxScore;
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
