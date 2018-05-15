package org.brewchain.browserAPI.block;

import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.PBLKCommand;
import org.brewchain.browserAPI.gens.Block.PBLKTModule;
import org.brewchain.browserAPI.gens.Block.ReqGetTheBestBlock;
import org.brewchain.browserAPI.gens.Block.ResGetTheBestBlock;

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
public class GetTheBestBlock extends SessionModules<ReqGetTheBestBlock> {

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GTB.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BLK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTheBestBlock pb, final CompleteHandler handler) {
		ResGetTheBestBlock.Builder ret = ResGetTheBestBlock.newBuilder();
		BlockInfo.Builder block = blockHelper.getTheBestBlock();
		if (block != null) {
			ret.setBlock(block);
		}
		ret.setRetCode(1);

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}

}
