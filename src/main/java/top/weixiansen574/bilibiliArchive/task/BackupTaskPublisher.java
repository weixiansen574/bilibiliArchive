package top.weixiansen574.bilibiliArchive.task;

public class BackupTaskPublisher {

    private TaskManger taskManger;
    private LoopThread thread;


    public BackupTaskPublisher(TaskManger taskManger) {
        this.taskManger = taskManger;
    }

    public void start(BackupTask backupTask, long interval) {
        if (thread == null) {
            thread = new LoopThread(taskManger, backupTask, interval);
            thread.setName("BackupTaskPublisher");
            thread.start();
        }
    }

    public void stop() {
        if(thread != null) {
            try {
                thread.running = false;
                thread.interrupt();
                thread.join();
            } catch (InterruptedException ignored) {
            }
            thread = null;
        }
    }

    public static class LoopThread extends Thread {

        public boolean running;
        private final long interval;
        private final TaskManger taskManger;
        private final BackupTask backupTask;

        public LoopThread(TaskManger taskManger,BackupTask backupTask, long interval) {
            this.interval = interval;
            this.taskManger = taskManger;
            this.backupTask = backupTask;
            running = true;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    taskManger.commitTask(backupTask);
                    Thread.sleep(interval);
                } catch (InterruptedException ignored) {
                }
            }
            System.out.println("备份任务发布器已停止循环！");
        }
    }
}
