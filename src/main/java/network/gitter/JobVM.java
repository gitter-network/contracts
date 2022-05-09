package network.gitter;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.List;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class JobVM extends Job {
    String name;
    List<Integer> paidFees;
    List<Integer> executions;
    int createdAt;
    int interval;
    int nextExec;

    public JobVM(Hash160 contract, String method, Hash160 creator, Object[] args, String name, List<Integer> paidFees,
            List<Integer> executions, int createdAt, int interval, int nextExec) {
        super(contract, method, creator, args);
        this.name = name;
        this.paidFees = paidFees;
        this.executions = executions;
        this.createdAt = createdAt;
        this.interval = interval;
        this.nextExec = nextExec;
    }
}
