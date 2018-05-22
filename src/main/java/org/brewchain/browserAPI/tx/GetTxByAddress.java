package org.brewchain.browserAPI.tx;


import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.browserAPI.Helper.BlockHelper;
import org.brewchain.browserAPI.gens.Tx.PTRSCommand;
import org.brewchain.browserAPI.gens.Tx.PTRSModule;
import org.brewchain.browserAPI.gens.Tx.ReqGetTxByAddress;
import org.brewchain.browserAPI.gens.Tx.ResGetTxByAddress;
import org.brewchain.browserAPI.gens.Tx.Transaction;
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
public class GetTxByAddress extends SessionModules<ReqGetTxByAddress>{

	@ActorRequire(name = "blockHelper", scope = "global")
	BlockHelper blockHelper;
	
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;
	
	@Override
	public String[] getCmds() {
		return new String[] { PTRSCommand.GTA.name() };
	}

	@Override
	public String getModule() {
		return PTRSModule.TRS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxByAddress pb, final CompleteHandler handler) {
		ResGetTxByAddress.Builder ret = ResGetTxByAddress.newBuilder();
		try{
			ret.setRetCode(1);
			
			if(pb != null && StringUtils.isNotBlank(pb.getAddress())){
				LinkedList<Transaction.Builder> txs = blockHelper.getTxByAddress(encApi.hexDec(pb.getAddress()));
				if(txs != null && !txs.isEmpty()){
					for(Transaction.Builder tx : txs){
						ret.addTransactions(tx);
					}
				}
			}
		} catch (Exception e){
			log.error("get tx error " + e.getMessage());
			ret.setRetCode(-1);
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
