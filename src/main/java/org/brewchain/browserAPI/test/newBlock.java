package org.brewchain.browserAPI.test;

import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqNbl;
import org.brewchain.browserAPI.gens.Lct.RetNbl;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

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
public class newBlock extends SessionModules<ReqNbl> {

	@ActorRequire(name = "Block_Helper", scope = "global")
	org.brewchain.account.core.BlockHelper oBlockHelper;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	private static int count = 0;

	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.NBL.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqNbl pb, final CompleteHandler handler) {
		RetNbl.Builder ret = RetNbl.newBuilder();

		BlockEntity.Builder oSyncBlock = BlockEntity.newBuilder();
		BlockEntity.Builder newBlock = null;

		try {
			newBlock = oBlockHelper.CreateNewBlock(600, ByteUtil.EMPTY_BYTE_ARRAY, ByteString.copyFromUtf8("12345").toByteArray());
		} catch (Exception e1) {
			e1.printStackTrace();
			log.debug("createNewBlock error : " + e1.getMessage());
		}
		oSyncBlock.setHeader(newBlock.getHeader());
		log.debug(String.format("==> 第 %s 块 hash %s 创建成功", oSyncBlock.getHeader().getNumber(), encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray())));
		try {
			oBlockHelper.ApplyBlock(oSyncBlock);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug(String.format("==> 第 %s 块 hash %s 父hash %s 交易 %s 笔", oSyncBlock.getHeader().getNumber(), encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray()), encApi.hexEnc(oSyncBlock.getHeader().getParentHash().toByteArray()), oSyncBlock.getHeader().getTxHashsCount()));
		count += 1;
		ret.setBlockHash(encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray()));
		ret.setRetCode(1);

		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
