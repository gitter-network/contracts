package network.gitter.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.neow3j.protocol.core.response.NeoInvokeFunction;
import io.neow3j.test.ContractTest;
import io.neow3j.types.ContractParameter;

@ContractTest(blockTime = 1, contracts = {
                GitterTreasury.class }, batchFile = "init.batch", configFile = "dev.neo-express")
public class TreasuryTest extends AbstractTest {

        private static final String GET_BALANCE = "getBalance";

        @Test
        public void paymentTest() throws Throwable {
                BigInteger transferAmount = BigInteger.valueOf(10_00000000);
                TestHelper.transfer17(gas, alice, treasury.getScriptHash(), transferAmount,
                                ContractParameter.any(null),
                                neow3j);

                NeoInvokeFunction result = treasury.callInvokeFunction(GET_BALANCE,
                                Arrays.asList(ContractParameter.hash160(alice)));

                assertEquals(transferAmount, result.getInvocationResult().getStack().get(0).getInteger());

        }
}
