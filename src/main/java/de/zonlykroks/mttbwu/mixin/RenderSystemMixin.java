package de.zonlykroks.mttbwu.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

	/**
	 * @author zOnlyKroks
	 * @reason Make Minecraft not complain
	 */
	@Overwrite(remap = false)
	public static boolean isOnRenderThread() {
		return true;
	}

}
