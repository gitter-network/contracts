package network.wizlo.contracts;

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
import network.wizlo.contracts.examples.CounterExample;

@ContractTest(blockTime = 1, contracts = { CounterExample.class, WizloRunner.class,
                WizloTreasury.class }, batchFile = "init.batch", configFile = "dev.neo-express")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RunnerTest extends AbstractTest {

        private static final String EXAMPLE_JOB_METHOD = "addCount";

        @Test
        @Order(1)
        public void createTimedJobTest() throws Throwable {
                runner.createTimedJob(1, 1, counterExample.getScriptHash(), EXAMPLE_JOB_METHOD, new Object[] { 1 },
                                alice.getScriptHash(), alice);

                InvocationResult jobsOfResult = runner.jobsOf(alice.getScriptHash());
                assertEquals(1, jobsOfResult.getStack().get(0).getIterator().size());
        }

        @Test
        @Order(2)
        public void executeJobNotEnoughBalanceTest() throws Throwable {
                Exception ex = assertThrows(Exception.class,
                                () -> runner.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
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
                                () -> runner.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 2 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));

                assertTrue(ex.getMessage().contains("noSuchJobFound"));
        }

        @Test
        @Order(4)
        public void createTimedJobAlreadyExistsForCreator() {

                Exception ex = assertThrows(Exception.class,
                                () -> runner.createTimedJob(1, 1, counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(), alice));
                assertTrue(ex.getMessage().contains("jobAlreadyExistsForCreator"));

        }

        @Test
        @Order(5)
        public void executeJobSuccess() throws Throwable {

                assertDoesNotThrow(
                                () -> runner.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
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
                                () -> runner.executeJob(counterExample.getScriptHash(), EXAMPLE_JOB_METHOD,
                                                new Object[] { 1 },
                                                alice.getScriptHash(),
                                                new byte[] { CallFlags.ALL.getValue() }, bob.getScriptHash(), alice));
                assertTrue(ex.getMessage().contains("tooEarly"));

        }

}
