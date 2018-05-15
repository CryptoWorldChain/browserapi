package org.brewchain.browserAPI.additional;


import java.util.List;

import org.brewchain.browserAPI.Helper.AdditionalHelper;
import org.brewchain.browserAPI.gens.Additional.Node;
import org.brewchain.browserAPI.gens.Additional.PADICommand;
import org.brewchain.browserAPI.gens.Additional.PADIModule;
import org.brewchain.browserAPI.gens.Additional.ReqGetNodes;
import org.brewchain.browserAPI.gens.Additional.ResGetNodes;

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
public class GetNodes extends SessionModules<ReqGetNodes>{

	@ActorRequire(name = "additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADICommand.GNS.name() };
	}

	@Override
	public String getModule() {
		return PADIModule.ADI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetNodes pb, final CompleteHandler handler) {
		ResGetNodes.Builder ret = ResGetNodes.newBuilder();
		List<Node> nodeList = additionalHelper.getNodes();
		
		if(nodeList != null && !nodeList.isEmpty()){
			for(Node node : nodeList){
				ret.addNodes(node);
			}
		}
		
		ret.setRetCode(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
