package org.brewchain.browserAPI.test;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.Helper.AdditionalHelper;
import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.Helper.WsServer;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqSSS;
import org.brewchain.browserAPI.gens.Lct.RetSSS;

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
public class StartSocketService extends SessionModules<ReqSSS> {

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;


	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.SSS.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSSS pb, final CompleteHandler handler) {
		RetSSS.Builder ret = RetSSS.newBuilder();

		String ip = "127.0.0.1";
		int port = 8888;
		if(pb != null){
			ip = StringUtils.isNotBlank(pb.getIp()) ? pb.getIp() : "127.0.0.1";
			port = pb.getPort();
		}
		WsServer socketService = WsServer.getInstance(ip, port, this.blockHelper, this.additionalHelper);
		try{
			socketService.start();
		} catch (Exception e){
			
		}
		
		
		ret.setRetCode(1);

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
