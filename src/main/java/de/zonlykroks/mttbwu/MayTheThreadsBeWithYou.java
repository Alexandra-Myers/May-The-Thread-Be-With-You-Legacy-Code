package de.zonlykroks.mttbwu;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MayTheThreadsBeWithYou implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("MayTheThreadsBeWithYou");

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("MayTheThreadsBeWithYou initialized");
	}
}
