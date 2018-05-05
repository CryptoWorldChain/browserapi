package org.brewchain.browserAPI.account;


import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.browserAPI.gens.AddressOuterClass.Address;
import org.brewchain.browserAPI.gens.AddressOuterClass.PADRCommand;
import org.brewchain.browserAPI.gens.AddressOuterClass.PADRModule;
import org.brewchain.browserAPI.gens.AddressOuterClass.ReqGetAddrDetailByAddr;
import org.brewchain.browserAPI.gens.AddressOuterClass.ResGetAddrDetailByAddr;
import org.fc.brewchain.bcapi.EncAPI;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.UUIDGenerator;

@NActorProvider
@Slf4j
@Data
public class GetAccountDetailByAddress extends SessionModules<ReqGetAddrDetailByAddr>{

	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	
	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper oBlockHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADRCommand.GAD.name() };
	}

	@Override
	public String getModule() {
		return PADRModule.ADR.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAddrDetailByAddr pb, final CompleteHandler handler) {
		ResGetAddrDetailByAddr.Builder ret = getReturn();
		
//		ResGetAddrDetailByAddr.Builder oRespGetAccount = ResGetAddrDetailByAddr.newBuilder();
//		oRespGetAccount.setRetCode(1);
//
//		Address.Builder addressEntity = Address.newBuilder();
		//TODO 未明确字段
//		addressEntity.setUsdValue("");
//		addressEntity.setComments("");
//		
//		try {
//			// 获取 address、 balance
//			Account oAccount = oAccountHelper.GetAccount(encApi.hexDec(pb.getAddress()));
//			if(oAccount != null){
//				if(oAccount.getAddress() != null) {
//					addressEntity.setAddress(oAccount.getAddress().toString());
//				}
//				if(oAccount.getValue() != null){
//					addressEntity.setBalance(oAccount.getValue().getBalance());
//				}
//			}
			
			//TODO 构造 transaction
//			BlockHelper.getTransactionByAddress();
			
//		} catch (Exception e) {
//			e.printStackTrace();
//			oRespGetAccount.setRetCode(-1);
//		}
//
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public ResGetAddrDetailByAddr.Builder getReturn() {
		ResGetAddrDetailByAddr.Builder ret = ResGetAddrDetailByAddr.newBuilder();
		Address.Builder addressEntity = Address.newBuilder();
		addressEntity.setAddress(UUIDGenerator.generate());
		addressEntity.setBalance(0L);
		addressEntity.setUsdValue("0.00");
		addressEntity.setComments("xxx");
		ret.setAddress(addressEntity);
		ret.setRetCode(1);
		return ret;
	}
	
	
}
