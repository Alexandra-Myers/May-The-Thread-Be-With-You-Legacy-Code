package de.zonlykroks.mttbwu.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin{

	@Unique
	private static Thread renderingThread;

	@Inject(method = "main([Ljava/lang/String;Z)V", at = @At(value = "INVOKE", target = "Ljava/lang/Runtime;addShutdownHook(Ljava/lang/Thread;)V"))
	private static void injectThread(String[] args, boolean bl, CallbackInfo ci) {
		Thread renderThread = new Thread() {
			@Override
			public void run() {
				super.run();
			}
		};

		renderThread.setName("Render Thread");

		renderingThread = renderThread;
	}

	@Redirect(method = "main([Ljava/lang/String;Z)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderThread()V"),remap = false)
	private static void initializeOffThread() {
		initRenderThread(renderingThread);
	}
	private static void initRenderThread(Thread thread) {
		if (renderingThread == null) {
			renderingThread = thread;
		} else {
			throw new IllegalStateException("Could not initialize render thread");
		}
	}
}
