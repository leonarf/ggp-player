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

    int mVisitCount;
    int mTotalScore;
    private MachineState mGameState;
    private List<MyMove> mChildren;

    private StateMachine mMachine;
    private Role mMyRole;
    private EnemyMove mParent;
}
