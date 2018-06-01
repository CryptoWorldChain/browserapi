package org.brewchain.browserAPI.test;


import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqAdd;
import org.brewchain.browserAPI.gens.Lct.RetAdd;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class AddBalance extends SessionModules<ReqAdd>{

	@ActorRequire(name = "Account_Helper", scope = "global")
	org.brewchain.account.core.AccountHelper oAccountHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.ADD.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqAdd pb, final CompleteHandler handler) {
		RetAdd.Builder ret = RetAdd.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
			if(StringUtils.isNotBlank(pb.getToken())){
				//erc20添加token
				try {
					oAccountHelper.addTokenBalance(encApi.hexDec(pb.getAddress()), pb.getToken(), pb.getBalance());
					ret.setRetCode(1);
				} catch (Exception e) {
					ret.setRetCode(-1).setMsg(e.getMessage());
				}
			} else if (StringUtils.isNotBlank(pb.getSymbol())){
				ArrayList<AccountCryptoToken.Builder> tokensList = new ArrayList<>();
				long currentTime = System.currentTimeMillis();
				for(int i = 1;i <= pb.getCount();i++){
					try {
						AccountCryptoToken.Builder oAccountCryptoToken = AccountCryptoToken.newBuilder();
						oAccountCryptoToken.setCode(String.valueOf(i));
						oAccountCryptoToken.setIndex( i - 1);
						oAccountCryptoToken.setName("name" + i);
						oAccountCryptoToken.setTimestamp(currentTime);
						oAccountCryptoToken.setTotal(pb.getCount());
						oAccountCryptoToken.setOwner(ByteString.copyFrom(encApi.hexDec(pb.getAddress())));
						oAccountCryptoToken.setNonce(0);
						oAccountCryptoToken.setHash(ByteString.copyFrom(encApi.sha256Encode(oAccountCryptoToken.build().toByteArray())));
						tokensList.add(oAccountCryptoToken);
					} catch (Exception e) {
						System.out.println("第【"+i+"】条数据错误，错误原因："+e.getMessage());
						log.debug("第【"+i+"】条数据错误，错误原因："+e.getMessage());
						continue;
					}
				}
				log.debug("拼装数据个数:"+tokensList.size());
				long doneCount = 0;
				try {
					doneCount = oAccountHelper.newCryptoBalances(encApi.hexDec(pb.getAddress()), pb.getSymbol(), tokensList);
				} catch (Exception e) {
					ret.setRetCode(-1).setMsg("error : " + e.getMessage());
				}
				ret.setRetCode(1);
				if(doneCount < pb.getCount()){
					ret.setMsg("warn, new cryptoToken count is not equals to " + pb.getCount());
				}
			}else{
				try {
					oAccountHelper.addBalance(encApi.hexDec(pb.getAddress()), pb.getBalance());
					org.brewchain.account.gens.Act.Account oAccount = oAccountHelper.GetAccount(encApi.hexDec(pb.getAddress()));
					ret.setRetCode(1).setMsg("success").setBalance(oAccount.getValue().getBalance());
				} catch (Exception e) {
					ret.setRetCode(-1).setMsg("error : " + e.getMessage());
				}
			}
		}else{
			ret.setRetCode(-1).setMsg("no address");
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
