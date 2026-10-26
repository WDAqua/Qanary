package eu.wdaqua.qanary.explainability.aspects;

import org.aspectj.lang.JoinPoint;

/**
 * Runs {@link QanaryAspect#setTriplestoreAndGraphForPipeline(JoinPoint)} on a
 * thread of its own.
 * <p>
 * The advice skips itself when any class on the current stack has "Test" in
 * its name, so a direct call from a test never reaches the pipeline code path.
 * This class deliberately has a neutral name and starts a fresh thread, whose
 * stack contains only this runner and the aspect, as in a running pipeline.
 */
final class PipelineAdviceRunner implements Runnable {

    private final QanaryAspect aspect;
    private final JoinPoint joinPoint;
    private Throwable failure;

    private PipelineAdviceRunner(QanaryAspect aspect, JoinPoint joinPoint) {
        this.aspect = aspect;
        this.joinPoint = joinPoint;
    }

    @Override
    public void run() {
        try {
            aspect.setTriplestoreAndGraphForPipeline(joinPoint);
        } catch (Throwable t) {
            failure = t;
        }
    }

    /** run the advice outside of any test class and rethrow what it threw */
    static void runOutsideOfTests(QanaryAspect aspect, JoinPoint joinPoint) throws Throwable {
        PipelineAdviceRunner runner = new PipelineAdviceRunner(aspect, joinPoint);
        Thread thread = new Thread(runner, "pipeline-advice");
        thread.start();
        thread.join();
        if (runner.failure != null) {
            throw runner.failure;
        }
    }
}
