package top.weixiansen574.bilibiliArchive;

public class StartupResult {
    public final Status status;
    public final Throwable error;

    public StartupResult(Status status, Throwable error) {
        this.status = status;
        this.error = error;
    }



    public enum Status {
        startSuccessful(1),
        hasStarted(2),
        failed(0);

        private final int value;

        Status(int i) {
            this.value = i;
        }

        public int getValue(){
            return value;
        }
    }


}
