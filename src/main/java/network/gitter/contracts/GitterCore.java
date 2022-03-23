package network.gitter.contracts;

import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.Transaction;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;
import io.neow3j.devpack.events.Event5Args;
import io.neow3j.devpack.events.Event6Args;

import static io.neow3j.devpack.Storage.getStorageContext;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;

import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.getTime;
import static io.neow3j.devpack.Runtime.getScriptContainer;
import static io.neow3j.devpack.Runtime.checkWitness;
import static io.neow3j.devpack.contracts.StdLib.serialize;
import static io.neow3j.devpack.contracts.StdLib.deserialize;
import static io.neow3j.devpack.contracts.CryptoLib.sha256;
import static io.neow3j.devpack.Storage.put;
import static io.neow3j.devpack.Storage.get;
import static io.neow3j.devpack.Storage.getIntOrZero;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;

@SuppressWarnings("unchecked")
@Permission(contract = "*", methods = "*")
@Permission(nativeContract = NativeContract.ContractManagement)
public class GitterCore {

    @DisplayName("JobCreated")
    private static Event5Args<ByteString, Hash160, String, Object[], Hash160> onJobCreated;

    @DisplayName("TimerSet")
    private static Event3Args<ByteString, Integer, Integer> onTimerSet;

    @DisplayName("ExecSuccess")
    private static Event6Args<Hash160, Hash160, String, Byte, Object[], Hash160> onExecution;

    @DisplayName("JobCancelled")
    private static Event2Args<ByteString, Hash160> onJobCancelled;

    @DisplayName("Debug")
    private static Event1Arg<Object> onDebug;

    private static final int MS_PER_MINUTE = 1000 * 60;
    private static final StorageContext ctx = getStorageContext();

    /* STORAGE MAPS */
    private static final StorageMap jobs = new StorageMap(ctx, toByteArray((byte) 2));
    private static final StorageMap timedJobs = new StorageMap(ctx, toByteArray((byte) 3));
    private static final StorageMap execAddresses = new StorageMap(ctx, toByteArray((byte) 4));

    /* STORAGE KEYS */
    private static final byte[] contractOwnerKey = toByteArray((byte) 5);
    private static final byte[] treasuryKey = toByteArray((byte) 6);
    private static final byte[] feeKey = toByteArray((byte) 7);

    /* READ ONLY */

    @Safe
    public static Hash160 contractOwner() {
        return new Hash160(get(ctx, contractOwnerKey));
    }

    @Safe
    public static Hash160 treasury() {
        return new Hash160(get(ctx, treasuryKey));
    }

    @Safe
    public static int fee() {
        return getIntOrZero(ctx, feeKey);
    }

    @Safe
    public static List<ByteString> jobsOf(Hash160 creator) {
        List<ByteString> jobs = new List<>();
        Iterator<ByteString> iterator = new StorageMap(ctx, creator.toByteArray()).find(FindOptions.KeysOnly);
        while (iterator.next()) {
            ByteString result = iterator.get();
            ByteString jobId = result.last(result.length() - creator.toByteArray().length);
            jobs.add(jobId);
        }
        return jobs;
    }

    static class Job {
        Hash160 contract;
        String method;
        Hash160 creator;
        Object[] args;

        Job(Hash160 contract, String method, Hash160 creator, Object[] args) {
            this.contract = contract;
            this.method = method;
            this.creator = creator;
            this.args = args;
        }
    }

    static class Timer {
        int nextExec;
        int interval;

        Timer(int nextExec, int interval) {
            this.interval = interval;
            this.nextExec = nextExec;
        }
    }

    public static void createTimedJob(
            int interval,
            int startTime,
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator) {
        assert (treasury() != null) : "noTreasurySet";
        assert (checkWitness(creator)) : "noAuth";
        ByteString job = createJob(contract, method, args, creator);
        interval *= MS_PER_MINUTE;
        int nextExec = startTime > getTime() ? startTime : getTime();
        timedJobs.put(job, serialize(new Timer(nextExec, interval)));
        onTimerSet.fire(job, nextExec, interval);
    }

    private static ByteString createJob(
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator) {
        ByteString job = getJobId(contract, method, creator, args);
        assert (jobs.get(job) == null) : "jobAlreadyExistsForCreator";
        jobs.put(job, creator);
        execAddresses.put(job, contract);
        new StorageMap(ctx, creator.toByteArray()).put(job, 1);
        onJobCreated.fire(job, contract, method, args, creator);
        return job;
    }

    public static void executeJob(
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator,
            byte callFlag,
            Hash160 executor) {
        assert (treasury() != null) : "noTreasurySet";
        int feeToPay = getTxCostsWithFee();
        boolean enoughBalance = getTreasuryBalanceOf(creator) >= feeToPay;
        assert (enoughBalance) : "creatorDoesNotHaveEnoughBalance";
        ByteString job = getJobId(contract, method, creator, args);
        onDebug.fire(job);
        assert (jobs.get(job) != null) : "noSuchJobFound";
        updateTimer(job);

        Contract.call(contract, method, callFlag, args);

        payExecutionFee(creator, executor, feeToPay);
        onExecution.fire(contract, executor, method, callFlag, args, creator);
    }

    public static void cancelJob(ByteString jobId) {
        ByteString result = jobs.get(jobId);
        assert (result != null) : "noJobFoundToCancel";
        Hash160 creator = new Hash160(result);
        assert (checkWitness(creator)) : "noAuthToCancelJob";
        execAddresses.delete(jobId);
        jobs.delete(jobId);
        new StorageMap(ctx, creator.toByteArray()).delete(jobId);
        timedJobs.delete(jobId);
        onJobCancelled.fire(jobId, creator);
    }

    public static void setTreasury(Hash160 treasury) {
        onlyOwner();
        boolean isTreasury = (boolean) Contract.call(treasury, "isTreasury", CallFlags.All, new Object[0]);
        assert (isTreasury) : "noTreasury";
        put(ctx, treasuryKey, treasury);
    }

    private static void updateTimer(ByteString job) {
        Timer timer = getTimer(job);
        boolean isTimedJob = timer != null;

        if (isTimedJob) {
            assert (timer.nextExec <= getTime()) : "tooEarly";

            // Skip to the next execution if its execution time is also now
            int nextExec = timer.nextExec + timer.interval;
            int timestamp = getTime();
            if (timestamp >= nextExec) {
                nextExec = nextExec + timer.interval;
            }
            timer.nextExec = nextExec;
            timedJobs.put(job, serialize(timer));
        }
    }

    private static int getTxCosts() {
        Transaction tx = (Transaction) getScriptContainer();
        return tx.networkFee + tx.systemFee;
    }

    private static int getTxCostsWithFee() {
        return getTxCosts() + fee();
    }

    private static ByteString getJobId(
            Hash160 contract,
            String method,
            Hash160 creator,
            Object[] args) {
        return sha256(serialize(new Job(contract, method, creator, args)));
    }

    private static Timer getTimer(ByteString job) {
        ByteString result = timedJobs.get(job);
        return result != null ? (Timer) deserialize(result) : null;
    }

    private static int getTreasuryBalanceOf(Hash160 from) {
        return (int) Contract.call(treasury(), "getBalance", CallFlags.All, new Object[] { from });
    }

    private static void payExecutionFee(Hash160 payer, Hash160 executor, int amount) {
        Contract.call(treasury(), "transfer", CallFlags.All, new Object[] { payer, executor, amount });
    }

    private static void onlyOwner() {
        assert (checkWitness(contractOwner())) : "onlyOwner";
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Object[] arr = (Object[]) data;
            Hash160 contractOwner = (Hash160) arr[0];
            int fee = (int) arr[1];
            put(ctx, contractOwnerKey, contractOwner);
            put(ctx, feeKey, fee);
        }
    }

}