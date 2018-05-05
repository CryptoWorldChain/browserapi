package org.brewchain.browserAPI.block;


import java.util.Date;

import org.brewchain.account.core.BlockHelper;
import org.brewchain.browserAPI.gens.BlockOuterClass.Block;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKCommand;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKTModule;
import org.brewchain.browserAPI.gens.BlockOuterClass.ReqGetBlockByTxHash;
import org.brewchain.browserAPI.gens.BlockOuterClass.ResGetBlockByTxHash;

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
public class GetBlockByTxHash extends SessionModules<ReqGetBlockByTxHash>{

	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper oBlockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GTH.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BLK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlockByTxHash pb, final CompleteHandler handler) {
		ResGetBlockByTxHash.Builder ret = getReturn();
//
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public ResGetBlockByTxHash.Builder getReturn() {
		ResGetBlockByTxHash.Builder ret = ResGetBlockByTxHash.newBuilder();
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
