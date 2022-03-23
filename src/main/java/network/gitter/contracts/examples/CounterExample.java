package network.gitter.contracts.examples;

import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.annotations.Safe;

import static io.neow3j.devpack.Storage.getStorageContext;

import io.neow3j.devpack.Storage;

import static io.neow3j.devpack.Helper.toByteArray;

public class CounterExample {

    private static final StorageContext ctx = getStorageContext();
    private static final byte[] counterKey = toByteArray((byte) 1);

    public static void addCount(int amount) {
        int newCounterValue = getCounterValue() + amount;
        Storage.put(ctx, counterKey, newCounterValue);
    }

    @Safe
    public static int getCounterValue() {
        return Storage.getIntOrZero(ctx, counterKey);
    }
}
