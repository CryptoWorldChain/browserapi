package org.brewchain.browserAPI.account;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.Helper.AddressHelper;
import org.brewchain.browserAPI.gens.Address.AddressInfo;
import org.brewchain.browserAPI.gens.Address.PADRCommand;
import org.brewchain.browserAPI.gens.Address.PADRModule;
import org.brewchain.browserAPI.gens.Address.ReqGetAddrDetailByAddr;
import org.brewchain.browserAPI.gens.Address.ResGetAddrDetailByAddr;

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
public class GetAccountDetailByAddress extends SessionModules<ReqGetAddrDetailByAddr>{

	@ActorRequire(name = "addressHelper", scope = "global")
	AddressHelper addressHelper;
	
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
		ResGetAddrDetailByAddr.Builder ret = ResGetAddrDetailByAddr.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
			AddressInfo.Builder addrInfo = addressHelper.getAccountDetailByAddress(pb.getAddress());
			if(addrInfo != null)
				ret.setAddress(addrInfo);
			ret.setRetCode(1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
