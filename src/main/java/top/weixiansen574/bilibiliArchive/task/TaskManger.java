package top.weixiansen574.bilibiliArchive.task;

import top.weixiansen574.bilibiliArchive.StartupResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskManger {
    private ExecutorService singleExecutorService;

    public TaskManger() {
    }

    public synchronized StartupResult start() {
        if (isRun()){
            System.out.println("任务管理器已在运行");
            return new StartupResult(StartupResult.Status.hasStarted,null);
        }
        singleExecutorService = Executors.newSingleThreadExecutor();
        return new StartupResult(StartupResult.Status.startSuccessful,null);
    }


    public boolean stop() throws InterruptedException {
        if (isRun()) {
            singleExecutorService.shutdown();
            boolean noTimeout = singleExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            singleExecutorService = null;
            return noTimeout;
        } else {
            return true;
        }
    }

    public boolean isRun() {
        return singleExecutorService != null && !singleExecutorService.isShutdown();
    }

    public void commitTask(Runnable runnable) {
        if (isRun()) {
            singleExecutorService.execute(runnable);
        } else {
            System.err.println("任务管理器已停止，提交任务失败！任务：" + runnable);
        }
    }

}
