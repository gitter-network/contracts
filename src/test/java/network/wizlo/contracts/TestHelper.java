package network.wizlo.contracts;

import java.math.BigInteger;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neow3j.contract.FungibleToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.response.NeoApplicationLog.Execution;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.Signer;
import io.neow3j.transaction.Transaction;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;

public class TestHelper {

    /* METHOD NAMES */
    protected static final String CREATE_JOB = "createJob";

    protected static final Logger log = LoggerFactory.getLogger(TestHelper.class);

    protected static void transfer17(
            FungibleToken token, Account from, Hash160 to, BigInteger amount, ContractParameter data, Neow3j neow3j)
            throws Throwable {
        Transaction tx = token.transfer(
                from, to, amount, data).sign();
        NeoSendRawTransaction res = tx.send();
        log.info("gas fee transfer17: {}\n", tx.getSystemFee() + tx.getNetworkFee());
        if (res.hasError()) {
            throw new Exception(res.getError().getMessage());
        }
        Await.waitUntilTransactionIsExecuted(res.getSendRawTransaction().getHash(), neow3j);
        log.info("{} transfer success: {}\n", token.getSymbol(), tx.getApplicationLog().getExecutions().get(0));
    }

    public static Execution invokeWrite(
            SmartContract contract, String method, List<ContractParameter> paramsList, Account signer, Neow3j neow3j)
            throws Throwable {
        Transaction tx = null;

        tx = contract.invokeFunction(method, paramsList.toArray(new ContractParameter[0]))
                .signers(new Signer[] { AccountSigner.calledByEntry(signer) }).sign();

        log.info("GAS FEE for {}: {}", method, tx.getSystemFee() + tx.getNetworkFee());
        NeoSendRawTransaction res = tx.send();
        if (res.hasError()) {
            log.info("error on tx.send {} : {}\n", method, res.getError().getMessage());
            throw new Exception(res.getError().getMessage());
        }
        Await.waitUntilTransactionIsExecuted(res.getSendRawTransaction().getHash(), neow3j);
        List<Execution> execs = tx.getApplicationLog().getExecutions();
        for (int i = 0; i < execs.size(); i++) {
            execs.get(i).getNotifications().forEach(n -> {
                log.info("{}: {}", n.getEventName(), n.getState().getList());
            });
            log.info("Exception: {}", execs.get(i).getException());
        }

        return tx.getApplicationLog().getExecutions().get(0);
    }

}
