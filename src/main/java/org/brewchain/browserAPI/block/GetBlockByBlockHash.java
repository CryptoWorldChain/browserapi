package org.brewchain.browserAPI.block;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.PBLKCommand;
import org.brewchain.browserAPI.gens.Block.PBLKTModule;
import org.brewchain.browserAPI.gens.Block.ReqGetBlockByBlockHash;
import org.brewchain.browserAPI.gens.Block.ResGetBlockByBlockHash;
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
public class GetBlockByBlockHash extends SessionModules<ReqGetBlockByBlockHash>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GHA.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BLK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByBlockHash pb, final CompleteHandler handler) {
		ResGetBlockByBlockHash.Builder ret = ResGetBlockByBlockHash.newBuilder();
		if(pb != null && StringUtils.isNotBlank(pb.getBlockHash())){
			BlockInfo.Builder block = blockHelper.getBlockByBlockHash(encApi.hexDec(pb.getBlockHash()));
			if(block != null)
				ret.setBlock(block);
			
			ret.setRetCode(1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
