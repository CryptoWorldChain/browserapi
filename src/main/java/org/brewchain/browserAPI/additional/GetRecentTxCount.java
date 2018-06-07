package org.brewchain.browserAPI.additional;


import org.brewchain.browserAPI.Helper.AdditionalHelper;
import org.brewchain.browserAPI.gens.Additional.PADICommand;
import org.brewchain.browserAPI.gens.Additional.PADIModule;
import org.brewchain.browserAPI.gens.Additional.ReqGetTxCount;
import org.brewchain.browserAPI.gens.Additional.ResGetTxCount;

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
public class GetRecentTxCount extends SessionModules<ReqGetTxCount>{

	@ActorRequire(name = "additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADICommand.GTC.name() };
	}

	@Override
	public String getModule() {
		return PADIModule.ADI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxCount pb, final CompleteHandler handler) {
		ResGetTxCount.Builder ret = ResGetTxCount.newBuilder();
//		additionalHelper.searchTx(ret, new Date().getTime());
		ret.setRetCode(-1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
