import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class MyMove {
	public MyMove(Move move, StoredState parent) throws MoveDefinitionException {
		mLegalMove = move;
		mParent = parent;
		mChildren = null;
	}

	public Move getMove() {
		return mLegalMove;
	}

	public StoredState getState() {
		return mParent;
	}

	public List<EnemyMove> getEnemyMoveList() throws MoveDefinitionException {
		if (mChildren == null) {
			List<List<Move>> legalJointMoves = mParent.getMachine()
					.getLegalJointMoves(mParent.getState(), mParent.getRole(),
							mLegalMove);
			mChildren = new ArrayList<EnemyMove>(legalJointMoves.size());
			for (List<Move> joinMove : legalJointMoves) {
				mChildren.add(new EnemyMove(joinMove, this));
			}
		}
		return mChildren;
	}

	private Move mLegalMove;
	private StoredState mParent;
	private List<EnemyMove> mChildren;
}
