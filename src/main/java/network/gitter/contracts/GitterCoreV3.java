package network.gitter.contracts;

import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.Transaction;
import io.neow3j.devpack.Iterator.Struct;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.constants.FindOptions;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.events.Event1Arg;
import io.neow3j.devpack.events.Event2Args;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event5Args;
import network.gitter.Job;
import network.gitter.JobVM;
import network.gitter.Timer;

import static io.neow3j.devpack.Storage.getStorageContext;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.List;
import io.neow3j.devpack.Map;

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
public class GitterCoreV3 {

    @DisplayName("JobCreated")
    private static Event5Args<ByteString, Hash160, String, Object[], Hash160> onJobCreated;

    @DisplayName("TimerSet")
    private static Event3Args<ByteString, Integer, Integer> onTimerSet;

    @DisplayName("ExecSuccess")
    private static Event5Args<Hash160, Hash160, String, Object[], Hash160> onExecution;

    @DisplayName("JobCancelled")
    private static Event2Args<ByteString, Hash160> onJobCancelled;

    @DisplayName("Debug")
    private static Event1Arg<Object> onDebug;

    private static final StorageContext ctx = getStorageContext();

    /* STORAGE MAPS */
    private static final StorageMap jobs = new StorageMap(ctx, toByteArray((byte) 2));
    private static final StorageMap timedJobs = new StorageMap(ctx, toByteArray((byte) 3));
    private static final StorageMap names = new StorageMap(ctx, toByteArray((byte) 5));
    private static final StorageMap timestamps = new StorageMap(ctx, toByteArray((byte) 11));

    /* STORAGE KEYS */
    private static final byte[] contractOwnerKey = toByteArray((byte) 6);
    private static final byte[] treasuryKey = toByteArray((byte) 7);
    private static final byte[] feeKey = toByteArray((byte) 8);

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
    public static List<ByteString> jobs() {
        Iterator<Struct<ByteString, ByteString>> iterator = jobs.find(FindOptions.RemovePrefix);
        List<ByteString> jobs = new List<>();
        while (iterator.next()) {
            jobs.add(iterator.get().key);
        }
        return jobs;
    }

    @Safe
    public static List<Map<ByteString, JobVM>> jobsOf(Hash160 creator) {
        List<Map<ByteString, JobVM>> jobs = new List<>();
        Iterator<Struct<ByteString, ByteString>> iterator = new StorageMap(ctx, creator.toByteArray())
                .find(FindOptions.RemovePrefix);
        while (iterator.next()) {
            Struct<ByteString, ByteString> result = iterator.get();
            Job job = (Job) deserialize(result.value);
            ByteString jobId = getJobId(job);
            ByteString timedJob = timedJobs.get(jobId);
            Timer timer = timedJob != null ? (Timer) deserialize(timedJob) : null;
            JobVM jobVM = new JobVM(job.contract, job.method, job.creator, job.args, getNameForJob(jobId),
                    getFeesForJob(jobId), getExecutionsForJob(jobId), getTimestampForJob(jobId),
                    timer != null ? timer.interval : 0,
                    timer != null ? timer.nextExec : 0);
            Map<ByteString, JobVM> map = new Map<>();
            map.put(result.key, jobVM);
            jobs.add(map);
        }
        return jobs;
    }

    @Safe
    public static int getTimestampForJob(ByteString job) {
        return timestamps.getIntOrZero(job);
    }

    @Safe
    public static String getNameForJob(ByteString job) {
        return names.getString(job);
    }

    @Safe
    public static List<Integer> getExecutionsForJob(ByteString jobId) {
        List<Integer> timestamps = new List<>();
        Iterator<Struct<ByteString, ByteString>> iterator = new StorageMap(ctx, jobId).find(FindOptions.RemovePrefix);
        while (iterator.next()) {
            int timestamp = iterator.get().key.toInt();
            timestamps.add(timestamp);
        }
        return timestamps;
    }

    @Safe
    public static List<Integer> getFeesForJob(ByteString jobId) {
        List<Integer> paidFees = new List<>();
        Iterator<ByteString> iterator = new StorageMap(ctx, jobId).find(FindOptions.ValuesOnly);
        while (iterator.next()) {
            int paidFee = iterator.get().toInt();
            paidFees.add(paidFee);
        }
        return paidFees;
    }

    @Safe
    public static JobVM getJob(ByteString jobId) {
        Job job = (Job) deserialize(jobs.get(jobId));
        ByteString timedJob = timedJobs.get(jobId);
        Timer timer = timedJob != null ? (Timer) deserialize(timedJob) : null;
        return new JobVM(job.contract, job.method, job.creator, job.args,
                getNameForJob(jobId),
                getFeesForJob(jobId),
                getExecutionsForJob(jobId),
                getTimestampForJob(jobId),
                timer != null ? timer.interval : 0,
                timer != null ? timer.nextExec : 0);
    }

    public static void createTimedJob(
            int interval,
            int startTime,
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator,
            String name) {
        assert (treasury() != null) : "noTreasurySet";
        assert (checkWitness(creator)) : "noAuth";
        ByteString job = createJob(contract, method, args, creator, name);
        int nextExec = startTime > getTime() ? startTime : getTime();
        timedJobs.put(job, serialize(new Timer(nextExec, interval)));
        onTimerSet.fire(job, nextExec, interval);
    }

    public static ByteString createJob(
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator,
            String name) {
        Job job = new Job(contract, method, creator, args);
        ByteString jobId = getJobId(job);
        assert (jobs.get(jobId) == null) : "jobAlreadyExistsForCreator";
        jobs.put(jobId, serialize(job));
        names.put(jobId, name);
        timestamps.put(jobId, getTime());
        new StorageMap(ctx, creator.toByteArray()).put(jobId, serialize(job));
        onJobCreated.fire(jobId, contract, method, args, creator);
        return jobId;
    }

    public static void executeJob(
            Hash160 contract,
            String method,
            Object[] args,
            Hash160 creator,
            Hash160 executor) {
        ByteString job = getJobId(new Job(contract, method, creator, args));
        executeJob(job, executor);
    }

    public static void executeJob(
            ByteString jobId,
            Hash160 executor) {
        assert (treasury() != null) : "noTreasurySet";
        Job job = (Job) deserialize(jobs.get(jobId));
        assert (jobs.get(jobId) != null) : "noSuchJobFound";

        int feeToPay = getTxCostsWithFee();
        boolean enoughBalance = getTreasuryBalanceOf(job.creator) >= feeToPay;
        assert (enoughBalance) : "creatorDoesNotHaveEnoughBalance";
        updateTimer(jobId);

        Contract.call(job.contract, job.method, CallFlags.All, job.args);

        new StorageMap(ctx, jobId).put(getTime(), feeToPay);
        payExecutionFee(job.creator, executor, feeToPay);
        onExecution.fire(job.contract, executor, job.method, job.args, job.creator);
    }

    public static void cancelJob(ByteString jobId) {
        ByteString result = jobs.get(jobId);
        assert (result != null) : "noJobFoundToCancel";
        Hash160 creator = new Hash160(result);
        assert (checkWitness(creator)) : "noAuthToCancelJob";
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

    private static ByteString getJobId(Job job) {
        return sha256(serialize(job));
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
            assert (contractOwner != null) : "ownerMustNotBeNull";
            int fee = (int) arr[1];
            assert (fee > 0) : "feeMustBeBiggerThanZero";
            put(ctx, contractOwnerKey, contractOwner);
            put(ctx, feeKey, fee);
        }
    }

    public static void update(ByteString script, String manifest) {
        onlyOwner();
        assert (script.length() != 0 || manifest.length() != 0) : "scriptOrManifestMissing";
        ContractManagement.update(script, manifest);
    }

}