import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

    static long MIN_MEMORY_AVAILABLE = 10485760; // 1024*1024*10 = 10 Mio
    int myRoleIndex;
    int roleCount;
    int movesSelectedCount;
    int mDepthLimit;
    int mMyMaxMobility;
    int mMaxEnemyMobility;
    int mMaxDepthReached;
    long mStartCalculation;
    long mAuthorizedTime;
    boolean mInitialization;
    boolean debugMode;
    int mMonteCarloProbeCount;

    StoredState mCurrentState;

    ArrayList<Long> depthTimeList = new ArrayList<Long>();
    ArrayList<Long> probeTimeList = new ArrayList<Long>();
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
	mStartCalculation = System.currentTimeMillis();
	mAuthorizedTime = timeout - 300;
	mInitialization = true;
	myRoleIndex = getStateMachine().getRoleIndices().get(getRole());
	roleCount = getStateMachine().getRoles().size();
	mCurrentState = new StoredState(getCurrentState(), getStateMachine(), getRole(), null);
	mDepthLimit = 0;
	depthTimeList.clear();
	long startDepthCalculation = mStartCalculation;
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
	mDepthLimit = depthTimeList.size() - 1;
	debugMode = false;
	mMonteCarloProbeCount = 4;
	System.out.println("mDepthLimit : " + mDepthLimit);
	mInitialization = false;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException,
	    GoalDefinitionException
    {
	mAuthorizedTime = timeout - 1000;
	mStartCalculation = System.currentTimeMillis();
	probeTimeList.clear();
	probeDepthList.clear();
	System.out.println("\n\ntime to think = " + (timeout - mStartCalculation));
	List<GdlTerm> gdlMoves = getMatch().getMostRecentMoves();
	if (gdlMoves != null)
	{
	    boolean enemyMovesFound = false;
	    for (MyMove myMove : mCurrentState.getMyLegalMoves())
	    {
		GdlTerm myLastGdlMove = gdlMoves.get(myRoleIndex);
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
	Move selection = null;
	// selection = getMiniMaxMove(mCurrentState);
	selection = getMonteCarloMove(mCurrentState);

	System.out.println("mMyMaxMobility : " + mMyMaxMobility);
	System.out.println("mMaxEnemyMobility : " + mMaxEnemyMobility);
	System.out.println("mMaxDepthReached : " + mMaxDepthReached);
	System.out.println("mDepthLimit : " + mDepthLimit);
	long totalProbeTime = 0;
	for (Long time : probeTimeList)
	{
	    totalProbeTime += time;
	}
	int totalProbeDepth = 0;
	for (Integer depth : probeDepthList)
	{
	    totalProbeDepth += depth;
	}
	String logAlphaBetaTimes = new String("Time to AlphaBeta per depth : ");
	for (int i = 0; i < depthTimeList.size(); ++i)
	{
	    logAlphaBetaTimes += depthTimeList.get(i) + "; ";
	}
	System.out.println(logAlphaBetaTimes);
	long averageProbeTime = 0;
	if (probeTimeList.size() > 0)
	{
	    averageProbeTime = totalProbeTime / probeTimeList.size();
	    System.out.println(probeTimeList.size() + " probes sent in " + totalProbeTime + "ms (average="
		    + averageProbeTime + "ms)");
	    System.out.println("Probes explored " + totalProbeDepth + " state (average=" + totalProbeDepth
		    / probeDepthList.size() + ")");
	} else
	{
	    System.out.println("No probe sent");
	}
	long timeLeft = mAuthorizedTime - System.currentTimeMillis();
	if (timeLeft > totalProbeTime && totalProbeTime > 0)
	{
	    System.out.println("mMonteCarloProbeCount was " + mMonteCarloProbeCount);
	    mMonteCarloProbeCount += timeLeft / totalProbeTime;
	    System.out.println("mMonteCarloProbeCount now is " + mMonteCarloProbeCount);
	}
	if (probeTimeList.size() > 0)
	{
	    while (mAuthorizedTime - (2 * averageProbeTime) > System.currentTimeMillis())
	    {
		int[] depth = new int[1];
		mCurrentState.sendProbe(depth);
	    }
	}
	System.out.println(timeLeft + " ms left before timeout");
	/* This will return Long.MAX_VALUE if there is no preset limit */
	long maxMemory = Runtime.getRuntime().maxMemory();
	/* Maximum amount of memory the JVM will attempt to use */
	System.out.println("Maximum memory (Mo): "
		+ (maxMemory == Long.MAX_VALUE ? "no limit" : (maxMemory / (1024 * 1024))));
	System.out.println("Total memory (Mo): " + Runtime.getRuntime().totalMemory() / (1024 * 1024));
	System.out
		.println("Memory available (Mo): " + (maxMemory - Runtime.getRuntime().totalMemory()) / (1024 * 1024));
	notifyObservers(new GamerSelectedMoveEvent(calculatedMoves, selection, timeLeft));
	System.gc();
	return selection;
    }

    public long getAvailableMemory()
    {
	long maxMemory = Runtime.getRuntime().maxMemory();
	if (maxMemory == Long.MAX_VALUE)
	{
	    return Long.MAX_VALUE;
	}
	return maxMemory - Runtime.getRuntime().totalMemory();
    }

    public Move getMonteCarloMove(StoredState currentState) throws GoalDefinitionException,
	    TransitionDefinitionException, MoveDefinitionException
    {
	if (getAvailableMemory() > MIN_MEMORY_AVAILABLE) // still a lot of memory
	{
		int[] depth = new int[1];
		while (!timeoutReached() && getAvailableMemory() > MIN_MEMORY_AVAILABLE)
		{
			long probeStartTime = System.currentTimeMillis();
			currentState.sendProbe(depth);
			probeTimeList.add(System.currentTimeMillis() - probeStartTime);
			probeDepthList.add(depth[0]);
		}
	}
	else // almost out of memory
	{
		System.out.println("Not enough memory to launch new probes. Returning average of " + currentState.getVisitCount()
				+ " previous probes");
	}
	int bestScore = -1;
	Move selection = currentState.getMyLegalMoves().get(0).getMove();
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
	    if (tmpMaxScore == 100)
	    {
		selection = currentState.getMyLegalMoves().get(i).getMove();
		break;
	    } else if (tmpMaxScore > miniMaxScore)
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
	// update enemy max mobility
	if (mInitialization && mMaxEnemyMobility < myMove.getEnemyMoveList().size())
	{
	    mMaxEnemyMobility = myMove.getEnemyMoveList().size();
	}
	for (int i = 0; i < myMove.getEnemyMoveList().size(); ++i)
	{
	    movesSelectedCount++;
	    StoredState nextState = myMove.getEnemyMoveList().get(i).getNextState();
	    int tmpMaxScore = getMaxScore(nextState, alpha, beta, depth + 1);
	    if (tmpMaxScore == 0)
	    {
		return 0;
	    } else if (tmpMaxScore < alpha)
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
	    char[] tab = new char[depth];
	    Arrays.fill(tab, '\t');
	    String profondeur = new String(tab);
	    logDebug(profondeur + "terminal state score : " + score);
	    return score;
	}
	// update max depth reached
	if (mInitialization && mMaxDepthReached < depth)
	{
	    mMaxDepthReached = depth;
	}
	if (depth > mDepthLimit || timeoutReached())
	{
	    if (mInitialization)
	    {
		return evaluateScore(currentState.getState());
	    } else
	    {
		return monteCarloEvaluation(currentState);
	    }
	}

	// update my max mobility
	if (mInitialization && mMyMaxMobility < currentState.getMyLegalMoves().size())
	{
	    mMyMaxMobility = currentState.getMyLegalMoves().size();
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

    public int monteCarloEvaluation(StoredState currentState) throws TransitionDefinitionException,
	    MoveDefinitionException, GoalDefinitionException
    {
	int[] depthToGo = new int[1];
	if (getAvailableMemory() < MIN_MEMORY_AVAILABLE) // almost out of memory
	{
	    System.out.println("Not enough memory for probe. Returning average of " + currentState.getVisitCount()
		    + " previous probes");
	    return currentState.getScore();
	}
	for (int i = 0; i < mMonteCarloProbeCount; ++i)
	{
	    long probeStartTime = System.currentTimeMillis();
	    currentState.sendProbe(depthToGo);
	    // monteCarloProbe(currentState, depthToGo);
	    probeTimeList.add(System.currentTimeMillis() - probeStartTime);
	    probeDepthList.add(depthToGo[0]);
	}
	return currentState.getScore();
    }

    public int monteCarloProbe(StoredState currentState, int[] depth) throws GoalDefinitionException,
	    TransitionDefinitionException, MoveDefinitionException
    {
	int score = 0;
	if (getStateMachine().isTerminal(currentState.getState()))
	{
	    score = getStateMachine().getGoal(currentState.getState(), getRole());
	    // System.out.println("Probe found terminal state with score = " +
	    // score);
	    currentState.UpdateScore(score);
	} else
	{
	    int legalMoveCount = currentState.getMyLegalMoves().size();
	    MyMove randomSelectedMove = currentState.getMyLegalMoves().get(new Random().nextInt(legalMoveCount));
	    legalMoveCount = randomSelectedMove.getEnemyMoveList().size();
	    EnemyMove randomSelectedMoves = randomSelectedMove.getEnemyMoveList().get(
		    new Random().nextInt(legalMoveCount));
	    ++depth[0];
	    score = monteCarloProbe(randomSelectedMoves.getNextState(), depth);
	}
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
	    // System.out.println("evaluateScore return " + score);
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
	// TODO Auto-generated method stub

    }

    @Override
    public void stateMachineAbort()
    {
	// TODO Auto-generated method stub

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

}
