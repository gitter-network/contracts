package network.gitter.contracts.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.protocol.core.response.NeoApplicationLog.Execution;

import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.wallet.Account;
import network.gitter.contracts.TestHelper;

public class GitterCoreContract extends SmartContract {

    private static final String CREATE_TIMED_JOB = "createTimedJob";
    private static final String EXECUTE_JOB = "executeJob";
    private static final String CANCEL_JOB = "cancelJob";
    private static final String jobsOf = "jobsOf";

    public GitterCoreContract(Hash160 scriptHash, Neow3j neow3j) {
        super(scriptHash, neow3j);
    }

    public Execution createTimedJob(int interval, int startTime, Hash160 contractHash, String method, Object[] args,
            Hash160 creatorHash, Account signerAccount) throws Throwable {
        ContractParameter _interval = ContractParameter.integer(interval);
        ContractParameter _startTime = ContractParameter.integer(startTime);
        ContractParameter _contract = ContractParameter.hash160(contractHash);
        ContractParameter _method = ContractParameter.string(method);
        ContractParameter _args = ContractParameter.array(args);
        ContractParameter _creator = ContractParameter.hash160(creatorHash);
        List<ContractParameter> params = Arrays.asList(_interval, _startTime, _contract, _method, _args, _creator);
        return TestHelper.invokeWrite(this, CREATE_TIMED_JOB, params, signerAccount, neow3j);
    }

    public Execution executeJob(Hash160 contractHash, String method, Object[] args,
            Hash160 creatorHash, byte[] callFlag, Hash160 executorHash, Account signerAccount) throws Throwable {
        ContractParameter _contract = ContractParameter.hash160(contractHash);
        ContractParameter _method = ContractParameter.string(method);
        ContractParameter _args = ContractParameter.array(args);
        ContractParameter _creator = ContractParameter.hash160(creatorHash);
        ContractParameter _callFlag = ContractParameter.byteArray(callFlag);
        ContractParameter _executor = ContractParameter.hash160(executorHash);
        List<ContractParameter> params = Arrays.asList(_contract, _method, _args, _creator, _callFlag, _executor);
        return TestHelper.invokeWrite(this, EXECUTE_JOB, params, signerAccount, neow3j);
    }

    public Execution cancelJob(byte[] job, Account signerAccount) throws Throwable {
        ContractParameter _job = ContractParameter.byteArray(job);
        List<ContractParameter> params = Arrays.asList(_job);
        return TestHelper.invokeWrite(this, CANCEL_JOB, params, signerAccount, neow3j);
    }

    public InvocationResult jobsOf(Hash160 account) throws IOException {
        return callInvokeFunction(jobsOf, Arrays.asList(ContractParameter.hash160(account))).getInvocationResult();

    }

}