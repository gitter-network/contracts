package network.gitter;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class Timer {
    public int nextExec;
    public int interval;

    public Timer(int nextExec, int interval) {
        this.interval = interval;
        this.nextExec = nextExec;
    }
}
