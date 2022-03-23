package network.gitter.contracts;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neow3j.contract.GasToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;

import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.test.DeployContext;
import io.neow3j.types.ContractParameter;
import io.neow3j.wallet.Account;
import network.gitter.contracts.examples.CounterExample;
import network.gitter.contracts.util.GitterCoreContract;

public abstract class AbstractTest {

        protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

        protected static Account alice;
        protected static Account bob;
        protected static Account charlie;

        @RegisterExtension
        protected static ContractTestExtension ext = new ContractTestExtension();

        protected static Neow3j neow3j;
        protected static GitterCoreContract core;
        protected static SmartContract treasury;
        protected static SmartContract counterExample;
        protected static GasToken gas;

        @DeployConfig(CounterExample.class)
        public static DeployConfiguration configureCounterExample() throws Exception {
                DeployConfiguration config = new DeployConfiguration();
                return config;
        }

        @DeployConfig(GitterTreasury.class)
        public static DeployConfiguration configureTreasury(DeployContext ctx) throws Exception {
                DeployConfiguration config = new DeployConfiguration();
                ContractParameter owner = ContractParameter
                                .hash160(ext.getAccount("NQcSTBwSJs7hcFUZzku2QdPNLe2dkTGok2").getScriptHash());
                ContractParameter core = ContractParameter
                                .hash160(ctx.getDeployedContract(GitterCore.class).getScriptHash());

                ContractParameter deployParams = ContractParameter
                                .array(owner, core);
                config.setDeployParam(deployParams);
                return config;
        }

        @DeployConfig(GitterCore.class)
        public static DeployConfiguration configureCore() throws Exception {
                DeployConfiguration config = new DeployConfiguration();

                ContractParameter deployParams = ContractParameter
                                .array(ContractParameter.hash160(
                                                ext.getAccount("NQcSTBwSJs7hcFUZzku2QdPNLe2dkTGok2").getScriptHash()),
                                                ContractParameter.integer(BigInteger.valueOf(1000000)));
                config.setDeployParam(deployParams);
                return config;
        }

        @BeforeAll
        public static void setup() throws Exception {
                neow3j = ext.getNeow3j();
                alice = ext.getAccount("NQcSTBwSJs7hcFUZzku2QdPNLe2dkTGok2");
                bob = ext.getAccount("NhsVB4etFffHjpLoj2ngVkkfNbtxiSSmbk");
                charlie = ext.getAccount("NdbtgSku2qLuwsBBzLx3FLtmmMdm32Ktor");
                gas = new GasToken(neow3j);

                counterExample = new SmartContract(ext.getDeployedContract(CounterExample.class).getScriptHash(),
                                ext.getNeow3j());
                treasury = new SmartContract(ext.getDeployedContract(GitterTreasury.class).getScriptHash(),
                                ext.getNeow3j());
                core = new GitterCoreContract(ext.getDeployedContract(GitterCore.class).getScriptHash(),
                                ext.getNeow3j());

                // set treasury in core contract
                try {
                        TestHelper.invokeWrite(core, "setTreasury",
                                        Arrays.asList(ContractParameter.hash160(treasury.getScriptHash())), alice,
                                        neow3j);
                } catch (Throwable e) {
                        e.printStackTrace();
                }

        }

}
