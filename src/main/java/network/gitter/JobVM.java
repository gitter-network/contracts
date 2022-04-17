package network.gitter;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class JobVM extends Job {
    String name;
    int paidFees;

    public JobVM(Hash160 contract, String method, Hash160 creator, Object[] args, String name, int paidFees) {
        super(contract, method, creator, args);
        this.name = name;
        this.paidFees = paidFees;
    }
}
