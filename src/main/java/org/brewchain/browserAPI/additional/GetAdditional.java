package org.brewchain.browserAPI.additional;


import org.brewchain.browserAPI.Helper.AdditionalHelper;
import org.brewchain.browserAPI.gens.Additional.PADICommand;
import org.brewchain.browserAPI.gens.Additional.PADIModule;
import org.brewchain.browserAPI.gens.Additional.ReqGetAdditional;
import org.brewchain.browserAPI.gens.Additional.ResGetAdditional;
import org.brewchain.evmapi.gens.Block.BlockEntity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
public class GetAdditional extends SessionModules<ReqGetAdditional>{

	@ActorRequire(name = "additionalHelper", scope = "global")
	AdditionalHelper additionalHelper;
	
	@Override
	public String[] getCmds() {
		return new String[] { PADICommand.GET.name() };
	}

	@Override
	public String getModule() {
		return PADIModule.ADI.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAdditional pb, final CompleteHandler handler) {
		ResGetAdditional.Builder ret = ResGetAdditional.newBuilder();
		ret = additionalHelper.getAdditional();
		ret.setRetCode(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
}
