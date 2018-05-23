package org.brewchain.browserAPI.test;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqAdd;
import org.brewchain.browserAPI.gens.Lct.RetAdd;
import org.fc.brewchain.bcapi.EncAPI;

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
			try {
				oAccountHelper.addBalance(encApi.hexDec(pb.getAddress()), pb.getBalance());
				org.brewchain.account.gens.Act.Account oAccount = oAccountHelper.GetAccount(encApi.hexDec(pb.getAddress()));
				ret.setRetCode(1).setMsg("success").setBalance(oAccount.getValue().getBalance());
			} catch (Exception e) {
				ret.setRetCode(-1).setMsg("error : " + e.getMessage());
			}
		}else{
			ret.setRetCode(-1).setMsg("no address");
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
