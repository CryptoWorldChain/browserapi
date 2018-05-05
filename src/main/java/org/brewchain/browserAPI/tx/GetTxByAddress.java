package org.brewchain.browserAPI.tx;


import java.util.Date;

import org.brewchain.account.core.BlockHelper;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKCommand;
import org.brewchain.browserAPI.gens.BlockOuterClass.PBLKTModule;
import org.brewchain.browserAPI.gens.Tx.ReqGetTxByAddress;
import org.brewchain.browserAPI.gens.Tx.ResGetTxByAddress;
import org.brewchain.browserAPI.gens.Tx.Transaction;

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
public class GetTxByAddress extends SessionModules<ReqGetTxByAddress>{

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
	public void onPBPacket(final FramePacket pack, final ReqGetTxByAddress pb, final CompleteHandler handler) {
		ResGetTxByAddress.Builder ret = getReturn();
//
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public ResGetTxByAddress.Builder getReturn() {
		ResGetTxByAddress.Builder ret = ResGetTxByAddress.newBuilder();
		Transaction.Builder tx = Transaction.newBuilder();
		tx.setActualTxCostFee(0.2d);
		tx.setBlockHeight(12L);
		tx.setFrom(UUIDGenerator.generate());
		tx.setGasLimit(100L);
		tx.setGasPrice(102.2d);
		tx.setGasUsedByTxn(12L);//TODO double
		tx.setInputData("a");
		tx.setNonce(2L);
		tx.setPrivateNote("");
		tx.setTimeStamp(new Date().getTime());
		tx.setTo(UUIDGenerator.generate());
		tx.setToken("TOKEN");
		tx.setTxHash(UUIDGenerator.generate());
		tx.setValue(120.2d);
		ret.setRetCode(1);
		return ret;
	}
}
