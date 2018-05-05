package org.brewchain.browserAPI.block;


import java.util.Date;

import org.brewchain.account.core.BlockHelper;
import org.brewchain.browserAPI.gens.BlockOuterClass.Block;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKCommand;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKTModule;
import org.brewchain.browserAPI.gens.BlockOuterClass.ReqGetBlockByBlockHash;
import org.brewchain.browserAPI.gens.BlockOuterClass.ResGetBlockByBlockHash;

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
public class GetBlockByBlockHash extends SessionModules<ReqGetBlockByBlockHash>{

	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper oBlockHelper;
	
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
		ResGetBlockByBlockHash.Builder ret = getReturn();
//
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public ResGetBlockByBlockHash.Builder getReturn() {
		ResGetBlockByBlockHash.Builder ret = ResGetBlockByBlockHash.newBuilder();
		Block.Builder block = Block.newBuilder();
		block.setHeight(1L);
		block.setTimeStamp(new Date().getTime());
		block.setTransactionsCount(12);
		block.setBlockHash(UUIDGenerator.generate());
		block.setParentHash(UUIDGenerator.generate());
		block.setSha3Uncles("");
		block.setMinedBy(UUIDGenerator.generate());
		block.setDifficulty(UUIDGenerator.generate());
		block.setTotalDifficulty(UUIDGenerator.generate());
		block.setSize(12L);
		block.setGasUsedP(0.9d);
		block.setGasLimit(100L);
		block.setNonce("1");
		block.setBlockReward(0.1d);
		block.setUnclesReward("0101");
		block.setExtraData("sss");
		ret.setBlock(block);
		ret.setRetCode(1);
		return ret;
	}
	
	
}
