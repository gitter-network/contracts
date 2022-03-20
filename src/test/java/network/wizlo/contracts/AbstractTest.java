package network.wizlo.contracts;

import java.util.Arrays;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.annotation.Contract;

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
import network.wizlo.contracts.examples.CounterExample;
import network.wizlo.contracts.util.WizloRunnerContract;

public abstract class AbstractTest {

    protected static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    protected static Account alice;
    protected static Account bob;
    protected static Account charlie;

    @RegisterExtension
    protected static ContractTestExtension ext = new ContractTestExtension();

    protected static Neow3j neow3j;
    protected static WizloRunnerContract runner;
    protected static SmartContract treasury;
    protected static SmartContract counterExample;
    protected static GasToken gas;

    @DeployConfig(CounterExample.class)
    public static DeployConfiguration configureCounterExample() throws Exception {
        DeployConfiguration config = new DeployConfiguration();
        return config;
    }

    @DeployConfig(WizloTreasury.class)
    public static DeployConfiguration configureTreasury(DeployContext ctx) throws Exception {
        DeployConfiguration config = new DeployConfiguration();
        ContractParameter owner = ContractParameter
                .hash160(ext.getAccount("NQcSTBwSJs7hcFUZzku2QdPNLe2dkTGok2").getScriptHash());
        ContractParameter runner = ContractParameter
                .hash160(ctx.getDeployedContract(WizloRunner.class).getScriptHash());

        ContractParameter deployParams = ContractParameter
                .array(owner, runner);
        config.setDeployParam(deployParams);
        return config;
    }

    @DeployConfig(WizloRunner.class)
    public static DeployConfiguration configureRunner() throws Exception {
        DeployConfiguration config = new DeployConfiguration();

        ContractParameter deployParams = ContractParameter
                .array(ContractParameter.hash160(ext.getAccount("NQcSTBwSJs7hcFUZzku2QdPNLe2dkTGok2").getScriptHash()));
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
        treasury = new SmartContract(ext.getDeployedContract(WizloTreasury.class).getScriptHash(),
                ext.getNeow3j());
        runner = new WizloRunnerContract(ext.getDeployedContract(WizloRunner.class).getScriptHash(),
                ext.getNeow3j());

        // set treasury in runner contract
        try {
            TestHelper.invokeWrite(runner, "setTreasury",
                    Arrays.asList(ContractParameter.hash160(treasury.getScriptHash())), alice, neow3j);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

}
