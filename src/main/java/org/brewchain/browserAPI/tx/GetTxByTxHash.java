package org.brewchain.browserAPI.tx;


import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Tx.PTRSCommand;
import org.brewchain.browserAPI.gens.Tx.PTRSModule;
import org.brewchain.browserAPI.gens.Tx.ReqGetTxByTxHash;
import org.brewchain.browserAPI.gens.Tx.ResGetTxByTxHash;
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
public class GetTxByTxHash extends SessionModules<ReqGetTxByTxHash>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PTRSCommand.GTT.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TRS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByTxHash pb, final CompleteHandler handler) {
		ResGetTxByTxHash.Builder ret = ResGetTxByTxHash.newBuilder();
		
		try{
			ret.setRetCode(1);
			if(pb != null && StringUtils.isNotBlank(pb.getTxHash())){
				ret.setTransaction(blockHelper.getTxByTxHash(encApi.hexDec(pb.getTxHash())));
			}
		} catch (Exception e){
			log.error("get tx error " + e.getMessage());
			ret.setRetCode(-1);
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
