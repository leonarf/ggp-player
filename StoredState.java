import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class StoredState {
    public StoredState(MachineState state, StateMachine machine, Role role, EnemyMove parent)
	    throws MoveDefinitionException
    {
	mGameState = state;
	mChildren = null;
	mMyRole = role;
	mMachine = machine;
	mParent = parent;
	mVisitCount = 0;
	mTotalScore = 0;
    }

    public MachineState getState()
    {
	return mGameState;
    }

    public List<MyMove> getMyLegalMoves() throws MoveDefinitionException
    {
	if (mChildren == null)
	{
	    calculatedLegalMove();
	}
	return mChildren;
    }

    public Role getRole()
    {
	return mMyRole;
    }

    public StateMachine getMachine()
    {
	return mMachine;
    }

    public void deleteParent()
    {
	mParent = null;
    }

    public void UpdateScore(int score)
    {
	++mVisitCount;
	mTotalScore += score;
    }

    public int getScore()
    {
	if (mVisitCount == 0)
	{
	    return 50;
	}
	return mTotalScore / mVisitCount;
    }

    public int sendProbe(int[] depth) throws GoalDefinitionException, TransitionDefinitionException,
	    MoveDefinitionException
    {
	if (mMachine.isTerminal(mGameState))
	{
	    if (mVisitCount == 0)
	    {
		++mVisitCount;
		mTotalScore = mMachine.getGoal(mGameState, mMyRole);
	    }
	    return mTotalScore;
	    // System.out.println("Probe found terminal state with score = " +
	    // score);
	} else
	{
	    ++mVisitCount;
	    List<Integer> choices = null;
	    int choosenMove = 0;
	    // Des mouvements n'ont jamais été explorés
	    if (mChildren == null)
	    {
		calculatedLegalMove();
	    }
	    if (mVisitCount < mChildren.size())
	    {
		choices = new ArrayList<>(mChildren.size() - mVisitCount);
		for (int i = 0; i < mChildren.size(); ++i)
		{
		    if (mChildren.get(i).getVisitCount() == 0)
		    {
			choices.add(i);
		    }
		}
		choosenMove = choices.get(new Random().nextInt(choices.size()));
	    }
	    // Tous les mouvements ont été explorés au moins une fois
	    else
	    {
		choosenMove = new Random().nextInt(mChildren.size());
	    }
	    int score = mChildren.get(choosenMove).sendProbe(depth);
	    mTotalScore += score;
	    return score;
	}
    }

    public int getVisitCount()
    {
	return mVisitCount;
    }

    private void calculatedLegalMove() throws MoveDefinitionException
    {
	List<Move> legalMoves = mMachine.getLegalMoves(mGameState, mMyRole);
	if (legalMoves.size() == 0)
	{
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    System.out.println("No legal move found :'(");
	    throw new MoveDefinitionException(mGameState, mMyRole);
	}
	mChildren = new ArrayList<MyMove>(legalMoves.size());
	for (Move move : legalMoves)
	{
	    mChildren.add(new MyMove(move, this));
	}
    }

    private int mVisitCount;
    private int mTotalScore;
    private MachineState mGameState;
    private List<MyMove> mChildren;

    private StateMachine mMachine;
    private Role mMyRole;
    private EnemyMove mParent;
}
