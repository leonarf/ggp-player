import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class PePePePlayer extends StateMachineGamer {

    static long MEGABYTE_TO_BYTE = 1048576; // 1024*1024 = 1 Mio
    static long MIN_MEMORY_AVAILABLE = MEGABYTE_TO_BYTE * 50; // 10 Mio
    static long MAX_PROBES_COUNT = 10000; // 10 Mio
    int myRoleIndex;
    int mDepthLimit;

    int mMyMaxMobility;
    int mMaxEnemyMobility;
    int mMaxDepthReached;

    /*
     * Time limit to stop searching tree and start returning chosen move
     */
    long mAuthorizedTime;

    boolean mInitialization;// Boolean true before game starts

    boolean debugMode;

    StoredState mCurrentState;// Save the current state of the game

    Move mMySelectedMove;

    ArrayList<Long> depthTimeList = new ArrayList<Long>();

    /*
     * Array containing the time of each probe, which were sent during the
     * current turn, took to find a terminal state
     */
    ArrayList<Long> probeTimeList = new ArrayList<Long>();

    /*
     * Array containing count of states explored by probes, during this turn, to
     * find a terminal state.
     */
    ArrayList<Integer> probeDepthList = new ArrayList<Integer>();

    @Override
    public StateMachine getInitialStateMachine()
    {
	return new CachedStateMachine(new ProverStateMachine());
    }

    public void logDebug(String log)
    {
	if (debugMode)
	{
	    System.out.println(log);
	}
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException,
	    GoalDefinitionException
    {
	System.out.println("\n\nNew game started!!!");
	System.out.println("Match id : " + getMatch().getMatchId());
	System.out.println("Play clock = " + getMatch().getPlayClock());
	System.out.println("Start clock = " + getMatch().getStartClock());
	System.out.println("Preview clock = " + getMatch().getPreviewClock() + "\n\n");
	/* This will return Long.MAX_VALUE if there is no preset limit */
	long maxMemory = Runtime.getRuntime().maxMemory();
	/* Maximum amount of memory the JVM will attempt to use */
	System.out.println("Maximum memory (Mo): "
		+ (maxMemory == Long.MAX_VALUE ? "no limit" : (maxMemory / MEGABYTE_TO_BYTE)));
	System.out.println("Total memory (Mo): "
		+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MEGABYTE_TO_BYTE);
	System.out.println("Memory available (Mo): "
		+ (maxMemory + Runtime.getRuntime().freeMemory() - Runtime.getRuntime().totalMemory())
		/ MEGABYTE_TO_BYTE);
	mInitialization = true;
	myRoleIndex = getStateMachine().getRoleIndices().get(getRole());
	mCurrentState = new StoredState(getCurrentState(), getStateMachine(), getRole(), null);
/*
	// Première phase : estimation de la profondeur qu'on peut analyser en
	// cour de partie
	long startDepthCalculation = System.currentTimeMillis();
	mAuthorizedTime = System.currentTimeMillis() + getMatch().getPlayClock() * 1000 - 300;
	mDepthLimit = 0;
	depthTimeList.clear();
	getMiniMaxMove(mCurrentState);
	long currentTime = System.currentTimeMillis();
	while (currentTime < mAuthorizedTime)
	{
	    depthTimeList.add(currentTime - startDepthCalculation);
	    System.out.println("AlphaBeta calcul time to depth " + mDepthLimit + " : " + depthTimeList.get(mDepthLimit)
		    + "ms");
	    mDepthLimit++;
	    startDepthCalculation = currentTime;
	    getMiniMaxMove(mCurrentState);
	    currentTime = System.currentTimeMillis();
	}
	System.out.println("AlphaBeta calcul time to depth " + mDepthLimit + " exceded timeout");
	mDepthLimit = 1;
	*/

	// Deuxième pahse : send as many probes as possible before game starts
	mAuthorizedTime = timeout - 300;
	sendProbes(mCurrentState,mAuthorizedTime);

	debugMode = false;
	mInitialization = false;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException,
	    GoalDefinitionException
    {
	mAuthorizedTime = timeout - 1000;
	System.out.println("\n\ntime to think = " + (timeout - System.currentTimeMillis()));

	// Update mCurrentState according to played moves
	List<GdlTerm> gdlMoves = getMatch().getMostRecentMoves();
	if (gdlMoves != null)
	{
	    boolean enemyMovesFound = false;
	    GdlTerm myLastGdlMove = gdlMoves.get(myRoleIndex);
	    if (!mMySelectedMove.getContents().equals(myLastGdlMove))
	    {
		System.out.println("My last selected move (" + mMySelectedMove.toString()
			+ ") different from last played move (" + myLastGdlMove.toString() + ")");
		System.out.println("My last selected move (" + mMySelectedMove.toString()
			+ ") different from last played move (" + myLastGdlMove.toString() + ")");
		System.out.println("My last selected move (" + mMySelectedMove.toString()
			+ ") different from last played move (" + myLastGdlMove.toString() + ")");
	    }
	    for (MyMove myMove : mCurrentState.getMyLegalMoves())
	    {
		// Looking for right MyMove
		if (myMove.getMove().getContents().equals(myLastGdlMove))
		{
		    for (EnemyMove enemyMove : myMove.getEnemyMoveList())
		    {
			enemyMovesFound = true;
			for (int i = 0; i < enemyMove.getJointMoves().size(); ++i)
			{
			    // Looking for right EnemyMove
			    if (!enemyMove.getJointMoves().get(i).getContents().equals(gdlMoves.get(i)))
			    {
				enemyMovesFound = false;
				break;
			    }
			}
			if (enemyMovesFound)
			{
			    mCurrentState = enemyMove.getNextState();
			    mCurrentState.deleteParent();
			    System.out.println("enemyMovefound : " + enemyMove.getJointMoves().toString());
			    break;
			}
		    }
		    break;
		}
	    }
	}

	List<Move> calculatedMoves = new ArrayList<>();
	for (MyMove myMove : mCurrentState.getMyLegalMoves())
	{
	    calculatedMoves.add(myMove.getMove());
	}
	System.out.println("possible moves calculated are : " + calculatedMoves.toString());
	if(calculatedMoves.size() > 1)
	{
	    mMySelectedMove = getMonteCarloMove(mCurrentState);
	    /*
	    sendProbes(mCurrentState, mAuthorizedTime - depthTimeList.get(mDepthLimit) - 500);
	    mMySelectedMove = getMiniMaxMove(mCurrentState);
	    */
	}
	else
	{
	    sendProbes(mCurrentState, mAuthorizedTime);
	    mMySelectedMove = calculatedMoves.get(0);
	}
	//mMySelectedMove = getMonteCarloMove(mCurrentState);

	long timeLeft = mAuthorizedTime - System.currentTimeMillis();
	System.out.println(timeLeft + " ms left before timeout");

	/* This will return Long.MAX_VALUE if there is no preset limit */
	long maxMemory = Runtime.getRuntime().maxMemory();
	/* Maximum amount of memory the JVM will attempt to use */
	System.out.println("Maximum memory (Mo): "
		+ (maxMemory == Long.MAX_VALUE ? "no limit" : (maxMemory / MEGABYTE_TO_BYTE)));
	System.out.println("Total memory (Mo): " + Runtime.getRuntime().totalMemory() / MEGABYTE_TO_BYTE);
	System.out.println("Memory available (Mo): " + (maxMemory - Runtime.getRuntime().totalMemory())
		/ MEGABYTE_TO_BYTE);
	notifyObservers(new GamerSelectedMoveEvent(calculatedMoves, mMySelectedMove, timeLeft));
	System.gc();
	return mMySelectedMove;
    }

    public long getAvailableMemory()
    {
	long maxMemory = Runtime.getRuntime().maxMemory();
	return maxMemory - Runtime.getRuntime().totalMemory();
    }

    public Move getMonteCarloMove(StoredState currentState) throws GoalDefinitionException,
	    TransitionDefinitionException, MoveDefinitionException
    {
	// mDepthLimit = 1;
	// expandState(currentState, 0);

	probeTimeList.clear();
	probeDepthList.clear();
	long totalProbeTime = 0;
	long totalProbeDepth = 0;
	int[] depth = new int[1];
	while (!timeoutReached() && probeTimeList.size() < MAX_PROBES_COUNT)
	{
	    depth[0] = 0;
	    long probeStartTime = System.currentTimeMillis();
	    if (getAvailableMemory() > MIN_MEMORY_AVAILABLE)
	    {
		currentState.sendProbe(depth);
	    } else
	    {
		System.out.println("Not enough memory to launch new probes. Sending simple probes");
		monteCarloProbe(currentState, depth);
	    }
	    probeTimeList.add(System.currentTimeMillis() - probeStartTime);
	    probeDepthList.add(depth[0]);
	    totalProbeTime += probeTimeList.get(probeTimeList.size() - 1);
	    totalProbeDepth += depth[0];
	}
	if (probeTimeList.size() > 0)
	{
	    long averageProbeTime = totalProbeTime / probeTimeList.size();
	    System.out.println(probeTimeList.size() + " probes sent in " + totalProbeTime + "ms (average="
		    + averageProbeTime + "ms)");
	    System.out.println("Probes explored " + totalProbeDepth + " state (average=" + totalProbeDepth
		    / probeDepthList.size() + ")");
	} else
	{
	    System.out.println("No probe sent");
	}
	int bestScore = -1;
	Move selection = null;
	for (MyMove move : currentState.getMyLegalMoves())
	{
	    if (move.getWorstScore() > bestScore)
	    {
		bestScore = move.getWorstScore();
		selection = move.getMove();
	    }
	}
	System.out.println("getMonteCarloMove choosing " + selection.toString() + " with score " + bestScore);
	return selection;
    }

    public Move getMiniMaxMove(StoredState currentState) throws MoveDefinitionException, GoalDefinitionException,
	    TransitionDefinitionException
    {
	Move selection = currentState.getMyLegalMoves().get(0).getMove();
	System.out.println("getMiniMaxMove : " + currentState.getMyLegalMoves().size() + " moves possible");
	int miniMaxScore = 0;
	for (int i = 0; i < currentState.getMyLegalMoves().size(); ++i)
	{
	    System.out.println("getMiniMaxMove : test move " + i + "/" + currentState.getMyLegalMoves().size() + ": "
		    + currentState.getMyLegalMoves().get(i).getMove().toString());
	    int tmpMaxScore = getMinScore(currentState.getMyLegalMoves().get(i), 0, 100, 0);
	    System.out.println("                 tmpMaxScore=" + tmpMaxScore);
	    if (tmpMaxScore > miniMaxScore)
	    {
		miniMaxScore = tmpMaxScore;
		selection = currentState.getMyLegalMoves().get(i).getMove();
	    }
	    if (timeoutReached())
	    {
		if (mDepthLimit > 0)
		{
		    mDepthLimit--;
		}
		System.out.println("Time out reached in getMiniMaxMove so mDepthLimit=" + mDepthLimit);
		break;
	    }
	}
	System.out.println("Choosing " + selection.toString() + " with score " + miniMaxScore);

	return selection;
    }

    public int getMinScore(MyMove myMove, int alpha, int beta, int depth) throws TransitionDefinitionException,
	    GoalDefinitionException, MoveDefinitionException
    {
	for (int i = 0; i < myMove.getEnemyMoveList().size(); ++i)
	{
	    StoredState nextState = myMove.getEnemyMoveList().get(i).getNextState();
	    int tmpMaxScore = getMaxScore(nextState, alpha, beta, depth + 1);
	    if (tmpMaxScore < alpha)
	    {
		return alpha;
	    } else if (tmpMaxScore < beta)
	    {
		beta = tmpMaxScore;
	    }
	    if (timeoutReached())
	    {
		break;
	    }
	}
	return beta;
    }

    public boolean timeoutReached()
    {
	if (System.currentTimeMillis() >= mAuthorizedTime)
	{
	    return true;
	}
	return false;
    }

    public int getMaxScore(StoredState currentState, int alpha, int beta, int depth)
	    throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException
    {
	if (getStateMachine().isTerminal(currentState.getState()))
	{
	    int score = getStateMachine().getGoal(currentState.getState(), getRole());
	    return score;
	}
	if (depth > mDepthLimit || timeoutReached())
	{
	    return monteCarloEvaluation(currentState);
	}
	for (int i = 0; i < currentState.getMyLegalMoves().size(); ++i)
	{
	    int tmpMinScore = getMinScore(currentState.getMyLegalMoves().get(i), alpha, beta, depth);
	    if (tmpMinScore == 100)
	    {
		return 100;
	    } else if (tmpMinScore > alpha)
	    {
		alpha = tmpMinScore;
	    } else if (tmpMinScore > beta)
	    {
		return beta;
	    }
	    if (timeoutReached())
	    {
		break;
	    }
	}
	return alpha;
    }

    public int monteCarloEvaluation(StoredState currentState) throws MoveDefinitionException,
	    TransitionDefinitionException
    {
	int bestScore = -1;
	for (MyMove move : currentState.getMyLegalMoves())
	{
	    if (move.getWorstScore() > bestScore)
	    {
		bestScore = move.getWorstScore();
		System.out.println("monteCarloEvaluation analyse move with " + move.getVisitCount() + " probes visited");
	    }
	}
	return bestScore;
    }

    public int monteCarloProbe(StoredState currentState, int[] depth) throws GoalDefinitionException,
	    TransitionDefinitionException, MoveDefinitionException
    {
	int score = 0;
	MachineState terminalState = getStateMachine().performDepthCharge(currentState.getState(), depth);
	score = getStateMachine().getGoal(terminalState, getRole());
	currentState.UpdateScore(score);
	return score;

    }

    public int evaluateScore(MachineState currentState) throws MoveDefinitionException
    {
	double score;
	try
	{
	    score = getStateMachine().getGoal(currentState, getRole());
	} catch (GoalDefinitionException e)
	{
	    int legalMovesCount = getStateMachine().getLegalMoves(currentState, getRole()).size();
	    if (legalMovesCount > mMyMaxMobility)
	    {
		mMyMaxMobility = legalMovesCount;
		score = 100;
	    } else
	    {
		score = (legalMovesCount * 100) / mMyMaxMobility;
	    }
	}
	if (score == 0)
	{
	    score++;
	}
	return (int) score;
    }

    @Override
    public void stateMachineStop()
    {
	int myScore;
	try
	{
	    myScore = getStateMachine().getGoal(getCurrentState(), getRole());
	    System.out.println("\n\nmatch finished! I got " + myScore + "\n\n");
	} catch (GoalDefinitionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	cleanUpEveryThing();
    }

    @Override
    public void stateMachineAbort()
    {
	cleanUpEveryThing();
	System.out.println("\n\nmatch aborted!");
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException
    {
	// TODO Auto-generated method stub

    }

    @Override
    public String getName()
    {
	return getClass().getSimpleName();
    }

    @Override
    public DetailPanel getDetailPanel()
    {
	return new SimpleDetailPanel();
    }

    private void cleanUpEveryThing()
    {
	cleanupAfterMatch();
	mCurrentState = null;
	mMySelectedMove = null;
	probeDepthList.clear();
	probeTimeList.clear();
	depthTimeList.clear();
	setMatch(null);
	System.gc();
    }

    private void sendProbes(StoredState currentState, long timeLimit ) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException
    {
	probeTimeList.clear();
	probeDepthList.clear();
	long totalProbeTime = 0;
	long totalProbeDepth = 0;
	int[] depth = new int[1];
	while (System.currentTimeMillis() < timeLimit && probeTimeList.size() < MAX_PROBES_COUNT)
	{
	    depth[0] = 0;
	    long probeStartTime = System.currentTimeMillis();
	    if (getAvailableMemory() > MIN_MEMORY_AVAILABLE)
	    {
		currentState.sendProbe(depth);
	    } else
	    {
		System.out.println("Not enough memory to launch new probes. Sending simple probes");
		monteCarloProbe(currentState, depth);
	    }
	    probeTimeList.add(System.currentTimeMillis() - probeStartTime);
	    probeDepthList.add(depth[0]);
	    totalProbeTime += probeTimeList.get(probeTimeList.size() - 1);
	    totalProbeDepth += depth[0];
	}
	if (probeTimeList.size() > 0)
	{
	    long averageProbeTime = totalProbeTime / probeTimeList.size();
	    System.out.println(probeTimeList.size() + " probes sent in " + totalProbeTime + "ms (average="
		    + averageProbeTime + "ms)");
	    System.out.println("Probes explored " + totalProbeDepth + " state (average=" + totalProbeDepth
		    / probeDepthList.size() + ")");
	} else
	{
	    System.out.println("No probe sent");
	}
    }

    private int expandState(StoredState rootState, int depth) throws MoveDefinitionException,
	    TransitionDefinitionException
    {
	int stateCount = 0;
	for (int i = 0; i < rootState.getMyLegalMoves().size(); ++i)
	{
	    for (int j = 0; j < rootState.getMyLegalMoves().get(i).getEnemyMoveList().size(); ++j)
	    {
		StoredState state = rootState.getMyLegalMoves().get(i).getEnemyMoveList().get(j).getNextState();
		if (depth == mDepthLimit)
		{
		    ++stateCount;
		} else
		{
		    stateCount += expandState(state, depth + 1);
		}
	    }
	}
	return stateCount;
    }
}
