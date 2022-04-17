package network.gitter;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.Struct;

@Struct
public class Job {
    public Hash160 contract;
    public String method;
    public Hash160 creator;
    public Object[] args;

    public Job(Hash160 contract, String method, Hash160 creator, Object[] args) {
        this.contract = contract;
        this.method = method;
        this.creator = creator;
        this.args = args;
    }
}
