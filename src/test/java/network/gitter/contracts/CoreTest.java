package network.gitter.contracts;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.neow3j.protocol.core.response.InvocationResult;
import io.neow3j.test.ContractTest;
import io.neow3j.types.CallFlags;
import io.neow3j.types.ContractParameter;
import network.gitter.contracts.examples.CounterExample;

@ContractTest(blockTime = 1, contracts = { CounterExample.class, GitterCoreV3.class,
                GitterTreasuryV4.class }, batchFile = "init.batch", configFile = "dev.neo-express")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CoreTest extends AbstractTest {

        private static final String EXAMPLE_JOB_METHOD = "addCount";

        @Test
        @Order(1)
        public void createTimedJobTest() throws Throwable {
                core.createTimedJob(1, 1, counterExample.getScriptHash(), EXAMPLE_JOB_METHOD, new Object[] { 1 },
                                alice.getScriptHash(), alice);

                InvocationResult jobsOfResult = core.jobsOf(alice.getScriptHash());
                assertEquals(1, jobsOfResult.getStack().get(0).getList().size());
        }

        @Test
        @Order(2)
        public void executeJobNotEnoughBalanceTest() throws Throwable {
                Exception ex = assertThrows(Exception.class,
                                () -> core.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));

                assertTrue(ex.getMessage().contains("creatorDoesNotHaveEnoughBalance"));
        }

        @Test
        @Order(3)
        public void executeJobNoSuchJobFoundTest() throws Throwable {
                TestHelper.transfer17(gas, alice, treasury.getScriptHash(), BigInteger.valueOf(5_00000000L),
                                ContractParameter.any(null),
                                neow3j);
                Exception ex = assertThrows(Exception.class,
                                () -> core.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 2 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));

                assertTrue(ex.getMessage().contains("noSuchJobFound"));
        }

        @Test
        @Order(4)
        public void createTimedJobAlreadyExistsForCreator() {

                Exception ex = assertThrows(Exception.class,
                                () -> core.createTimedJob(1, 1, counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(), alice));
                assertTrue(ex.getMessage().contains("jobAlreadyExistsForCreator"));

        }

        @Test
        @Order(5)
        public void executeJobSuccess() throws Throwable {

                assertDoesNotThrow(
                                () -> core.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));

                InvocationResult counterValueResult = counterExample.callInvokeFunction("getCounterValue")
                                .getInvocationResult();
                assertEquals(BigInteger.ONE, counterValueResult.getStack().get(0).getInteger());

        }

        @Test
        @Order(6)
        public void executeJobTooEarlyTest() throws Throwable {

                Exception ex = assertThrows(Exception.class,
                                () -> core.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));
                assertTrue(ex.getMessage().contains("tooEarly"));
        }

        @Test
        @Order(7)
        public void successfullyCancelJobTest() throws Throwable {
                byte[] job = core.jobsOf(alice.getScriptHash()).getStack().get(0).getList().get(0).getByteArray();
                assertDoesNotThrow(() -> core.cancelJob(job, alice));
        }

        @Test
        public void noJobFoundToCancelTest() throws Throwable {
                Exception ex = assertThrows(Exception.class,
                                () -> core.cancelJob(new byte[] { (byte) 2 }, alice));
                assertTrue(ex.getMessage().contains("noJobFoundToCancel"));
        }

}
