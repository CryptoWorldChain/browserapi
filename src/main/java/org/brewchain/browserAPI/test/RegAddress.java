package org.brewchain.browserAPI.test;


import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqReg;
import org.brewchain.browserAPI.gens.Lct.RetReg;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

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
public class RegAddress extends SessionModules<ReqReg>{

	@ActorRequire(name = "Account_Helper", scope = "global")
	org.brewchain.account.core.AccountHelper oAccountHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.REG.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqReg pb, final CompleteHandler handler) {
		RetReg.Builder ret = RetReg.newBuilder();
		
		KeyPairs oKeyPairs = encApi.genKeys();
		
		oAccountHelper.CreateAccount(encApi.hexDec(oKeyPairs.getAddress()), encApi.hexDec(oKeyPairs.getPubkey()));
		
		ret.setRetCode(1).setMsg("success");
		ret.setAddress(oKeyPairs.getAddress());
		ret.setPub(oKeyPairs.getPubkey());
		ret.setPki(oKeyPairs.getPrikey());
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
