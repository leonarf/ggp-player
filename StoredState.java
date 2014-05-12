import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class StoredState {
    public StoredState(MachineState state, StateMachine machine, Role role, EnemyMove parent) throws MoveDefinitionException
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
		    List<Move> legalMoves = mMachine.getLegalMoves(mGameState, mMyRole);
		    mChildren = new ArrayList<MyMove>(legalMoves.size());
		    for (Move move : legalMoves)
		    {
			mChildren.add(new MyMove(move, this));
		    }
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
    	if(mParent != null)
    	{
    		mParent.getMyMove().getState().UpdateScore(score);
    	}
    }

    public int getScore()
    {
    	if(mVisitCount == 0 || mTotalScore == 0)
    	{
    		return 0;
    	}
    	return mTotalScore/mVisitCount;
    }

    private int mVisitCount;
    private int mTotalScore;
    private MachineState mGameState;
    private List<MyMove> mChildren;

    private StateMachine mMachine;
    private Role mMyRole;
    private EnemyMove mParent;
}
