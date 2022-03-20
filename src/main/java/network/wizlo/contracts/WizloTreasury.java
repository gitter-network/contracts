package network.wizlo.contracts;

import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.NativeContract;
import io.neow3j.devpack.contracts.GasToken;
import io.neow3j.devpack.events.Event3Args;
import io.neow3j.devpack.events.Event4Args;

import static io.neow3j.devpack.Storage.getStorageContext;
import static io.neow3j.devpack.Storage.put;

import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Storage;

import static io.neow3j.devpack.Helper.toByteArray;
import static io.neow3j.devpack.Runtime.getCallingScriptHash;
import static io.neow3j.devpack.Runtime.getExecutingScriptHash;
import static io.neow3j.devpack.Runtime.checkWitness;

@Permission(contract = "*", methods = "*")
@Permission(nativeContract = NativeContract.ContractManagement)
public class WizloTreasury {

    @DisplayName("Payment")
    private static Event3Args<Hash160, Integer, Object> onPayment;

    @DisplayName("TreasuryTransfer")
    private static Event4Args<Hash160, Hash160, Integer, Integer> onTreasuryTransfer;

    private static final StorageContext ctx = getStorageContext();

    /* STORAGE MAPS */
    private static final StorageMap gasAssets = new StorageMap(ctx, toByteArray((byte) 1));

    /* STORAGE KEYS */
    private static final byte[] contractOwnerKey = toByteArray((byte) 2);
    private static final byte[] runnerKey = toByteArray((byte) 3);

    @Safe
    public static int getBalance(Hash160 from) {
        return gasAssets.getIntOrZero(from.toByteArray());
    }

    @Safe
    public static boolean isTreasury() {
        return true;
    }

    @OnNEP17Payment
    public static void onPayment(Hash160 sender, int amount, Object data) {
        assert (getCallingScriptHash() == GasToken.getHash()) : "onlyGas";
        addToBalance(sender, amount);
        onPayment.fire(sender, amount, data);
    }

    public static void transfer(Hash160 from, Hash160 to, int amount) {
        onlyRunner();
        int newBalance = deductFromBalance(from, amount);
        GasToken.transfer(getExecutingScriptHash(), to, amount, null);
        onTreasuryTransfer.fire(from, to, amount, newBalance);
    }

    private static void addToBalance(Hash160 to, int amount) {
        int newBalance = getBalance(to) + amount;
        gasAssets.put(to.toByteArray(), newBalance);
    }

    private static int deductFromBalance(Hash160 from, int amount) {
        int newBalance = getBalance(from) - amount;
        assert (newBalance >= 0) : "notEnoughBalance";
        gasAssets.put(from.toByteArray(), newBalance);
        return newBalance;
    }

    private static void onlyRunner() {
        Hash160 runner = new Hash160(Storage.get(ctx, runnerKey));
        assert (checkWitness(runner)) : "onlyRunner";
    }

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Object[] arr = (Object[]) data;
            Hash160 contractOwner = (Hash160) arr[0];
            Hash160 runner = (Hash160) arr[1];
            put(ctx, contractOwnerKey, contractOwner);
            put(ctx, runnerKey, runner);
        }
    }
}