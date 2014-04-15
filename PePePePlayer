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
		Move selection = moves.get(0);
		int maxScore = 0;
		for( int i=0; i < moves.size(); ++i)
		{
			int tmpMaxScore = getMaxScore(moves.get(i), getCurrentState());
			if( tmpMaxScore == 100)
			{
				selection = moves.get(i);
				break;
			}
			else if(tmpMaxScore > maxScore)
			{
				maxScore = tmpMaxScore;
				selection = moves.get(i);
			}
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;		// TODO Auto-generated method stub
	}

	public int getMaxScore(Move move, MachineState currentState)
			throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
	{
		int maxScore = 0;
		List<Move> moveList = Arrays.asList(move);
		MachineState nextState = getStateMachine().getNextState(currentState, moveList);
		if(getStateMachine().isTerminal(nextState))
		{
			return getStateMachine().getGoal(nextState, getRole());
		}

		List<Move> legalMoves = getStateMachine().getLegalMoves(nextState, getRole());
		for( int i=0; i < legalMoves.size(); ++i)
		{
			int tmpMaxScore = getMaxScore(legalMoves.get(i), nextState);
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
