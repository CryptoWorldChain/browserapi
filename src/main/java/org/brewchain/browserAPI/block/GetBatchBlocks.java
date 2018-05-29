package org.brewchain.browserAPI.block;


import java.util.List;

import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Block.BlockInfo;
import org.brewchain.browserAPI.gens.Block.PBLKCommand;
import org.brewchain.browserAPI.gens.Block.PBLKTModule;
import org.brewchain.browserAPI.gens.Block.ReqGetBatchBlocks;
import org.brewchain.browserAPI.gens.Block.ResGetBatchBlocks;

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
public class GetBatchBlocks extends SessionModules<ReqGetBatchBlocks>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PBLKCommand.GBB.name() };
	}

	@Override
	public String getModule() {
		return PBLKTModule.BLK.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBatchBlocks pb, final CompleteHandler handler) {
		ResGetBatchBlocks.Builder ret = ResGetBatchBlocks.newBuilder();
		//默认参数
		int pageNo = 1;
		int pageSize = 10;// 暂定 10 行
		if(pb != null){
			if(pb.getPageNo() > 0){
				pageNo = pb.getPageNo();
			}
			if(pb.getPageSize() > 0){
				pageSize = pb.getPageSize();
			}
		}
		
		ret.setTotalCount(blockHelper.getTheBestBlockHeight());
		
		List<BlockInfo.Builder> list = blockHelper.getBatchBlocks(pageNo, pageSize);
		
		if(list != null && !list.isEmpty()){
			for (BlockInfo.Builder block : list) {
				ret.addBlocks(block);
			}
		}
		
		ret.setRetCode(1);
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
