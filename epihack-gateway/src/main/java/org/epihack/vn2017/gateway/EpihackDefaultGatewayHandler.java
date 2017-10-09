package org.epihack.vn2017.gateway;

import com.mario.entity.impl.BaseMessageHandler;
import com.mario.entity.message.Message;
import com.nhb.common.data.MapTuple;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.PuObject;
import com.nhb.common.data.PuObjectRO;

public class EpihackDefaultGatewayHandler extends BaseMessageHandler {

	@Override
	public void init(PuObjectRO initParams) {
		getLogger().debug("Start epihack default gateway handler with params: ", initParams);
	}

	@Override
	public void destroy() throws Exception {
		// no nothing
	}

	@Override
	public PuElement handle(Message message) {
		return PuObject.fromObject(new MapTuple<>("status", 1, "message", "Unknown error"));
	}
}
