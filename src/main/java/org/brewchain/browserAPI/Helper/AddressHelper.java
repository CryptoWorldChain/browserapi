package org.brewchain.browserAPI.Helper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.evmapi.gens.Act.Account;
import org.brewchain.evmapi.gens.Act.AccountCryptoToken;
import org.brewchain.evmapi.gens.Act.AccountCryptoValue;
import org.brewchain.evmapi.gens.Act.AccountTokenValue;
import org.brewchain.evmapi.gens.Act.AccountValue;
import org.brewchain.browserAPI.gens.Address.AddressInfo;
import org.brewchain.browserAPI.gens.Address.CryptoToken;
import org.brewchain.browserAPI.gens.Address.CryptoTokenValue;
import org.brewchain.browserAPI.gens.Address.Token;
import org.brewchain.browserAPI.gens.Tx.Transaction;
import org.brewchain.browserAPI.util.DataUtil;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

/**
 * @author jack
 * 
 *         address 相关信息获取
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "addressHelper")
@Slf4j
@Data
public class AddressHelper implements ActorService {
	
	@ActorRequire(name = "Account_Helper", scope = "global")
	org.brewchain.account.core.AccountHelper accountHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;

	/**
	 * @param address
	 * @return
	 */
	public AddressInfo.Builder getAccountDetailByAddress(String address) {
		AddressInfo.Builder account = null;

		Account oAccount = accountHelper.GetAccount(encApi.hexDec(address));
		if (oAccount != null) {
			account = AddressInfo.newBuilder();
			AccountValue oAccountValue = oAccount.getValue();
			if (oAccountValue != null) {
				// nonce
				account.setNonce(oAccountValue.getNonce());

				// balance
				account.setBalance(oAccountValue.getBalance());

				// address
				if (oAccountValue.getAddressList() != null && !oAccountValue.getAddressList().isEmpty()) {
					for (ByteString str : oAccountValue.getAddressList()) {
						account.addAddress(DataUtil.byteString2String(str, encApi));
					}
				}

				// tokens
				if (oAccountValue.getTokensList() != null && !oAccountValue.getTokensList().isEmpty()) {
					for (AccountTokenValue t : oAccountValue.getTokensList()) {
						Token.Builder token = Token.newBuilder();
						token.setBalance(t.getBalance());
						token.setToken(StringUtils.isNotBlank(t.getToken()) ? t.getToken() : "");
						account.addTokens(token);
					}
				}

				// cryptoToken
				if (oAccountValue.getCryptosList() != null && !oAccountValue.getCryptosList().isEmpty()) {
					for (AccountCryptoValue cv : oAccountValue.getCryptosList()) {
						CryptoToken.Builder cryptoToken = CryptoToken.newBuilder();
						cryptoToken.setSymbol(StringUtils.isNotBlank(cv.getSymbol()) ? cv.getSymbol() : "");
						List<AccountCryptoToken> acts = cv.getTokensList();
						if (acts != null && !acts.isEmpty()) {
							for (AccountCryptoToken act : acts) {
								CryptoTokenValue.Builder ctv = CryptoTokenValue.newBuilder();
								ctv.setHash(DataUtil.byteString2String(act.getHash(), encApi));
								ctv.setTimestamp(act.getTimestamp());
								ctv.setIndex(act.getIndex());
								ctv.setTotal(act.getTotal());
								ctv.setCode(StringUtils.isNotBlank(act.getCode()) ? act.getCode() : "");
								ctv.setName(StringUtils.isNotBlank(act.getName()) ? act.getName() : "");
								ctv.setOwner(DataUtil.byteString2String(act.getOwner(), encApi));
								ctv.setNonce(act.getNonce());

								cryptoToken.addTokens(ctv);
							}
							account.addCryptoTokens(cryptoToken);
						}
					}
				}

				// transactions
				Map<String, Transaction> map = blockHelper.getTxByAddress(address);
				
				Iterator<String> it = map.keySet().iterator();
				while(it.hasNext()){
					String key = it.next();
					account.addTransactions(map.get(key));
				}
			}
		}

		return account;
	}

}
