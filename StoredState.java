import java.util.List;

import org.ggp.base.util.statemachine.MachineState;


public class StoredState {
	int mVisitCount;
	int mTotalScore;
	private MachineState mGameState;
	private List<MyMove> mChildren;
}
