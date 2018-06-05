package org.brewchain.browserAPI.test;


import java.util.List;

import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqGBS;
import org.brewchain.browserAPI.gens.Lct.RetGBS;

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
public class GetBlocksHash extends SessionModules<ReqGBS>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.GBS.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGBS pb, final CompleteHandler handler) {
		RetGBS.Builder ret = RetGBS.newBuilder();
		List<String> list = blockHelper.getBlocksHash();
		for(String str : list){
			ret.addHashs(str);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
