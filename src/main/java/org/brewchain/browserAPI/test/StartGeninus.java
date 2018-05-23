package org.brewchain.browserAPI.test;


import java.util.LinkedList;

import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.browserAPI.gens.Lct.PLCTCommand;
import org.brewchain.browserAPI.gens.Lct.PLCTModule;
import org.brewchain.browserAPI.gens.Lct.ReqSat;
import org.brewchain.browserAPI.gens.Lct.RetSat;

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
public class StartGeninus extends SessionModules<ReqSat>{

	@ActorRequire(name = "Block_Helper", scope = "global")
	org.brewchain.account.core.BlockHelper oBlockHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PLCTCommand.SAT.name() };
	}

	@Override
	public String getModule() {
		return PLCTModule.LCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSat pb, final CompleteHandler handler) {
		RetSat.Builder ret = RetSat.newBuilder();
		if (pb.getBlock() == 1) {
			// 创建创世块
			try {
				oBlockHelper.CreateGenesisBlock(new LinkedList<MultiTransaction>(), ByteUtil.EMPTY_BYTE_ARRAY);
				ret.setRetCode(1).setMsg("success");
			} catch (Exception e) {
				ret.setRetCode(-1).setMsg("error : " + e.getMessage());
			}
		}
		
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
