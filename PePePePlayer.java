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
			//selection = getMoveAccordingCompulsiveDeliberation(moves);
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
		System.out.println("getMiniMaxMove : " + legalMoves.size() + " moves possible");
		int miniMaxScore = 0;
		if(legalMoves.size() > 1)
		{
			for( int i=0; i < legalMoves.size(); ++i)
			{
				profondeur = "";
				ArrayList<Move> moveList = new ArrayList<Move>();
				moveList.add(legalMoves.get(i));
				System.out.println("\n\n getMiniMaxMove : " + legalMoves.get(i).toString());
				int tmpMaxScore = getMinScore(moveList, getCurrentState(), 1, 0, 100);
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
			}
		}
		System.out.println("Choosing " + selection.toString() + "\n\n");

		return selection;
	}

	public int getMinScore(List<Move> playersMoves, MachineState currentState, int role, int alpha, int beta)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		List<Move> legalEnemyMoves = getStateMachine().getLegalMoves(currentState, getStateMachine().getRoles().get(role));
		for( int i=0; i < legalEnemyMoves.size(); ++i)
		{
			System.out.println(profondeur + "getMinScore : " + legalEnemyMoves.get(i).toString() + " " + legalEnemyMoves.size() + " possible moves");
			playersMoves.add(legalEnemyMoves.get(i));
			MachineState nextState = getStateMachine().getNextState(currentState, playersMoves);
			profondeur += "  ";
			int tmpMaxScore = getMaxScore(nextState, alpha, beta);
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
		}
		return beta;
	}

	public int getMaxScore(MachineState currentState, int alpha, int beta)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		if(getStateMachine().isTerminal(currentState))
		{
			int score = getStateMachine().getGoal(currentState, getRole());
			System.out.println(profondeur + "terminal state score : " + score);
			return score;
		}

		List<Move> legalMoves = getStateMachine().getLegalMoves(currentState, getRole());
		for( int i=0; i < legalMoves.size(); ++i)
		{
			System.out.println(profondeur + "getMaxScore : " + legalMoves.get(i).toString() + " " + legalMoves.size() + " possible moves");
			ArrayList<Move> moveList = new ArrayList<Move>();
			moveList.add(legalMoves.get(i));
			profondeur += "  ";
			int tmpMinScore = getMinScore(moveList, currentState, moveList.size(), alpha, beta);
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
		}
		return alpha;
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
