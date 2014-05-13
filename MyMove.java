import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MyMove {
    public MyMove(Move move, StoredState parent) throws MoveDefinitionException
    {
	mLegalMove = move;
	mParent = parent;
	mChildren = null;
	mVisitCount = 0;
    }

    public Move getMove()
    {
	return mLegalMove;
    }

    public StoredState getState()
    {
	return mParent;
    }

    public List<EnemyMove> getEnemyMoveList() throws MoveDefinitionException
    {
	if (mChildren == null)
	{
	    calculateEnemyMoves();
	}
	return mChildren;
    }

    public int getVisitCount()
    {
    	return mVisitCount;
    }

    public int sendProbe(int[] depth) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException
    {
    	++mVisitCount;
		List<Integer> choices = null;
		int choosenMove = 0;
		//Des mouvements n'ont jamais été explorés
		if(mChildren == null)
		{
			calculateEnemyMoves();
		}
		if( mVisitCount < mChildren.size())
		{
			choices = new ArrayList<>(mChildren.size() - mVisitCount);
			for(int i=0; i < mChildren.size(); ++i)
			{
				if(mChildren.get(i).getVisitCount() == 0)
				{
					choices.add(i);
				}
			}
			choosenMove = choices.get(new Random().nextInt(choices.size()));
		}
		//Tous les mouvements ont été explorés au moins une fois
		else
		{
			choosenMove = new Random().nextInt(mChildren.size());
		}
	    return mChildren.get(choosenMove).getNextState().sendProbe(depth);
    }

    private void calculateEnemyMoves() throws MoveDefinitionException
    {
    	List<List<Move>> legalJointMoves = mParent.getMachine().getLegalJointMoves(mParent.getState(),
    		    mParent.getRole(), mLegalMove);
	    mChildren = new ArrayList<EnemyMove>(legalJointMoves.size());
	    for (List<Move> joinMove : legalJointMoves)
	    {
		mChildren.add(new EnemyMove(joinMove, this));
	    }
    }

    private Move mLegalMove;
    private StoredState mParent;
    private List<EnemyMove> mChildren;
    private int mVisitCount;
}
