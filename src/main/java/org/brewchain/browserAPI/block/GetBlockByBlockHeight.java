package org.brewchain.browserAPI.block;

import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.PBLKCommand;
import org.brewchain.browserAPI.gens.Block.PBLKTModule;
import org.brewchain.browserAPI.gens.Block.ReqGetBlockByBlockHeight;
import org.brewchain.browserAPI.gens.Block.ResGetBlockByBlockHeight;

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
public class GetBlockByBlockHeight extends SessionModules<ReqGetBlockByBlockHeight>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GHE.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BLK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByBlockHeight pb, final CompleteHandler handler) {
		ResGetBlockByBlockHeight.Builder ret = ResGetBlockByBlockHeight.newBuilder();
		if(pb != null && pb.getBlockHeight() >= 0){
			BlockInfo.Builder block = blockHelper.getBlockByBlockHeight(pb.getBlockHeight());
			if(block != null)
				ret.setBlock(block);
			
			ret.setRetCode(1);
		}
//
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
}
