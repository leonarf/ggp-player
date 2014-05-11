import java.util.List;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class EnemyMove {
    public EnemyMove(List<Move> jointMoves, MyMove parent)
    {
	mJointMoves = jointMoves;
	mParent = parent;
	mChild = null;
    }

    public StoredState getNextState() throws TransitionDefinitionException, MoveDefinitionException
    {
	if (mChild == null)
	{
	    mChild = new StoredState(mParent.getState().getMachine()
		    .getNextState(mParent.getState().getState(), mJointMoves), mParent.getState().getMachine(), mParent
		    .getState().getRole(), this);
	}
	return mChild;
    }

    public List<Move> getJointMoves()
    {
	return mJointMoves;
    }

    private List<Move> mJointMoves;
    private MyMove mParent;
    private StoredState mChild;
}
